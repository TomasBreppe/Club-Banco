package com.example.demo.service.impl;

import com.example.demo.entity.CategoriaIngresoManual;
import com.example.demo.entity.IngresoManualEntity;
import com.example.demo.entity.MedioIngresoManual;
import com.example.demo.entity.MedioPago;
import com.example.demo.entity.PagoEntity;
import com.example.demo.repository.GastoRepository;
import com.example.demo.repository.IngresoManualRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.ExcelBalanceService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelBalanceServiceImpl implements ExcelBalanceService {

    private final PagoRepository pagoRepository;
    private final IngresoManualRepository ingresoManualRepository;
    private final GastoRepository gastoRepository;

    private static final List<String> ORDEN_BLOQUES = List.of(
            "CUOTA SOCIETARIA",
            "INSCRIPCION",
            "RECARGOS",
            "ALQUILER",
            "BASQUET",
            "HANDBALL",
            "PATIN",
            "RITMICA",
            "TAEKWONDO",
            "KARATE",
            "FUTBOL");

    @Override
    public byte[] generarExcelBalance(LocalDate desde, LocalDate hasta) {
        try (Workbook workbook = new XSSFWorkbook()) {

            boolean hayFiltroFecha = desde != null || hasta != null;
            LocalDate hoy = LocalDate.now();

            LocalDate fechaDesde = hayFiltroFecha
                    ? (desde != null ? desde : LocalDate.of(2000, 1, 1))
                    : hoy.withDayOfMonth(1);

            LocalDate fechaHasta = hayFiltroFecha
                    ? (hasta != null ? hasta : LocalDate.of(2999, 12, 31))
                    : hoy.withDayOfMonth(hoy.lengthOfMonth());

            LocalDateTime desdeDt = fechaDesde.atStartOfDay();
            LocalDateTime hastaDt = fechaHasta.atTime(LocalTime.MAX);

            Sheet sheet = workbook.createSheet("Ingresos");
            configurarHoja(sheet);

            Map<String, CellStyle> styles = crearStyles(workbook);

            List<MovimientoExcel> movimientos = construirMovimientos(fechaDesde, fechaHasta, desdeDt, hastaDt);

            int rowNum = 0;

            rowNum = escribirEncabezado(sheet, rowNum, fechaDesde, fechaHasta, styles);

            Map<String, List<MovimientoExcel>> porBloque = movimientos.stream()
                    .collect(Collectors.groupingBy(
                            MovimientoExcel::getBloque,
                            LinkedHashMap::new,
                            Collectors.toList()));

            BigDecimal totalGeneral = BigDecimal.ZERO;
            Map<String, BigDecimal> totalesGeneralesPorMedio = new LinkedHashMap<>();
            totalesGeneralesPorMedio.put("Efectivo", BigDecimal.ZERO);
            totalesGeneralesPorMedio.put("Tarjeta", BigDecimal.ZERO);
            totalesGeneralesPorMedio.put("Transferencias", BigDecimal.ZERO);

            for (String bloque : ORDEN_BLOQUES) {
                List<MovimientoExcel> items = porBloque.getOrDefault(bloque, Collections.emptyList());

                if (items.isEmpty()) {
                    continue;
                }

                ResultadoBloque resultado = escribirBloque(sheet, rowNum, bloque, items, styles);
                rowNum = resultado.getSiguienteFila();

                totalGeneral = totalGeneral.add(resultado.getTotalBloque());

                for (Map.Entry<String, BigDecimal> e : resultado.getTotalesPorMedio().entrySet()) {
                    totalesGeneralesPorMedio.merge(e.getKey(), e.getValue(), BigDecimal::add);
                }
            }

            rowNum = escribirTotalesGenerales(sheet, rowNum, totalesGeneralesPorMedio, totalGeneral, styles);

            ajustarColumnas(sheet);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel", e);
        }
    }

    private List<MovimientoExcel> construirMovimientos(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            LocalDateTime desdeDt,
            LocalDateTime hastaDt) {
        List<MovimientoExcel> out = new ArrayList<>();

        List<PagoEntity> pagos = pagoRepository.buscarDashboardSinMedio(desdeDt, hastaDt, "");

        for (PagoEntity p : pagos) {
            if (p == null || Boolean.TRUE.equals(p.getAnulado())) {
                continue;
            }

            String medio = mapMedioPago(p.getMedio());

            if ("INSCRIPCION".equalsIgnoreCase(p.getConcepto() != null ? p.getConcepto() : "")) {
                out.add(new MovimientoExcel(
                        "INSCRIPCION",
                        "CLUB BANCO",
                        "INSCRIPCION",
                        "INSCRIPCION",
                        medio,
                        medio,
                        safe(p.getMontoTotal())));
                continue;
            }

            if ("CUOTA_MENSUAL".equalsIgnoreCase(p.getConcepto() != null ? p.getConcepto() : "")) {

                if (safe(p.getMontoSocial()).compareTo(BigDecimal.ZERO) > 0) {
                    out.add(new MovimientoExcel(
                            "CUOTA SOCIETARIA",
                            "CLUB BANCO",
                            "CUOTA SOCIETARIA",
                            "CUOTA SOCIETARIA",
                            medio,
                            medio,
                            safe(p.getMontoSocial())));
                }

                BigDecimal montoDisciplina = safe(p.getMontoDisciplina()).add(safe(p.getMontoPreparacionFisica()));
                String disciplina = normalizarDisciplina(
                        p.getDisciplina() != null ? p.getDisciplina().getNombre() : null);

                if (montoDisciplina.compareTo(BigDecimal.ZERO) > 0 && ORDEN_BLOQUES.contains(disciplina)) {
                    out.add(new MovimientoExcel(
                            disciplina,
                            "CLUB BANCO",
                            disciplina,
                            disciplina,
                            medio,
                            medio,
                            montoDisciplina));
                }
            }
        }

        List<IngresoManualEntity> ingresos = ingresoManualRepository.findAll();

        for (IngresoManualEntity i : ingresos) {
            if (i == null || i.getFecha() == null) {
                continue;
            }

            if (i.getFecha().isBefore(fechaDesde) || i.getFecha().isAfter(fechaHasta)) {
                continue;
            }

            String bloque = mapCategoriaIngresoManual(i.getCategoria());
            if (bloque == null) {
                continue;
            }

            String medio = mapMedioIngresoManual(i.getMedioPago());

            out.add(new MovimientoExcel(
                    bloque,
                    "CLUB BANCO",
                    bloque,
                    bloque,
                    medio,
                    medio,
                    safe(i.getMonto())));
        }

        return out;
    }

    private int escribirEncabezado(
            Sheet sheet,
            int rowNum,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Map<String, CellStyle> styles) {
        Row rowTitulo = sheet.createRow(rowNum++);
        rowTitulo.setHeightInPoints(24);
        crearCelda(rowTitulo, 0, "CLUB BANCO", styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(rowTitulo.getRowNum(), rowTitulo.getRowNum(), 0, 5));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Row rowSub = sheet.createRow(rowNum++);
        crearCelda(
                rowSub,
                0,
                "CORDOBA - CORDOBA - (CORDOBA)",
                styles.get("subtitle"));
        sheet.addMergedRegion(new CellRangeAddress(rowSub.getRowNum(), rowSub.getRowNum(), 0, 3));
        crearCelda(
                rowSub,
                4,
                "Fecha: " + LocalDateTime.now().format(dtf),
                styles.get("subtitleRight"));
        sheet.addMergedRegion(new CellRangeAddress(rowSub.getRowNum(), rowSub.getRowNum(), 4, 5));

        Row rowFiltro = sheet.createRow(rowNum++);
        crearCelda(
                rowFiltro,
                0,
                "Período: " + fechaDesde.format(df) + " al " + fechaHasta.format(df),
                styles.get("filter"));
        sheet.addMergedRegion(new CellRangeAddress(rowFiltro.getRowNum(), rowFiltro.getRowNum(), 0, 5));

        rowNum++;
        return rowNum;
    }

    private ResultadoBloque escribirBloque(
            Sheet sheet,
            int rowNum,
            String bloque,
            List<MovimientoExcel> items,
            Map<String, CellStyle> styles) {
        Row bloqueRow = sheet.createRow(rowNum++);
        crearCelda(bloqueRow, 0, bloque, styles.get("blockTitle"));
        sheet.addMergedRegion(new CellRangeAddress(bloqueRow.getRowNum(), bloqueRow.getRowNum(), 0, 5));

        Row header = sheet.createRow(rowNum++);
        crearCelda(header, 0, "SEDE", styles.get("header"));
        crearCelda(header, 1, "CENTRO COSTO", styles.get("header"));
        crearCelda(header, 2, "RUBRO", styles.get("header"));
        crearCelda(header, 3, "F. PAGO", styles.get("header"));
        crearCelda(header, 4, "MEDIO PAGO", styles.get("header"));
        crearCelda(header, 5, "TOTAL", styles.get("header"));

        Map<String, BigDecimal> porMedio = new LinkedHashMap<>();
        porMedio.put("Efectivo", BigDecimal.ZERO);
        porMedio.put("Tarjeta", BigDecimal.ZERO);
        porMedio.put("Transferencias", BigDecimal.ZERO);

        BigDecimal totalBloque = BigDecimal.ZERO;

        Map<String, List<MovimientoExcel>> agrupado = items.stream()
                .collect(Collectors.groupingBy(
                        MovimientoExcel::getMedioPago,
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (String medio : List.of("Efectivo", "Tarjeta", "Transferencias")) {
            List<MovimientoExcel> lista = agrupado.getOrDefault(medio, Collections.emptyList());
            if (lista.isEmpty()) {
                continue;
            }

            BigDecimal subtotal = BigDecimal.ZERO;

            for (MovimientoExcel m : lista) {
                Row row = sheet.createRow(rowNum++);
                crearCelda(row, 0, m.getSede(), styles.get("cell"));
                crearCelda(row, 1, m.getCentroCosto(), styles.get("cell"));
                crearCelda(row, 2, m.getRubro(), styles.get("cell"));
                crearCelda(row, 3, m.getFormaPago(), styles.get("cell"));
                crearCelda(row, 4, m.getMedioPago(), styles.get("cell"));
                crearCeldaMoneda(row, 5, m.getTotal(), styles.get("money"));

                subtotal = subtotal.add(m.getTotal());
            }

            Row subtotalRow = sheet.createRow(rowNum++);
            crearCelda(subtotalRow, 0, "", styles.get("subtotalLabel"));
            crearCelda(subtotalRow, 1, "", styles.get("subtotalLabel"));
            crearCelda(subtotalRow, 2, "", styles.get("subtotalLabel"));
            crearCelda(subtotalRow, 3, "", styles.get("subtotalLabel"));
            crearCelda(subtotalRow, 4, "TOTAL " + medio.toUpperCase() + " ->", styles.get("subtotalLabel"));
            crearCeldaMoneda(subtotalRow, 5, subtotal, styles.get("subtotalMoney"));

            porMedio.put(medio, subtotal);
            totalBloque = totalBloque.add(subtotal);
        }

        Row totalRow = sheet.createRow(rowNum++);
        crearCelda(totalRow, 0, "", styles.get("totalLabel"));
        crearCelda(totalRow, 1, "", styles.get("totalLabel"));
        crearCelda(totalRow, 2, "", styles.get("totalLabel"));
        crearCelda(totalRow, 3, "", styles.get("totalLabel"));
        crearCelda(totalRow, 4, "TOTAL ->", styles.get("totalLabel"));
        crearCeldaMoneda(totalRow, 5, totalBloque, styles.get("totalMoney"));

        rowNum++;

        return new ResultadoBloque(rowNum, totalBloque, porMedio);
    }

    private int escribirTotalesGenerales(
            Sheet sheet,
            int rowNum,
            Map<String, BigDecimal> totalesPorMedio,
            BigDecimal totalGeneral,
            Map<String, CellStyle> styles) {
        Row titulo = sheet.createRow(rowNum++);
        crearCelda(titulo, 0, "TOTALES GENERALES", styles.get("blockTitle"));
        sheet.addMergedRegion(new CellRangeAddress(titulo.getRowNum(), titulo.getRowNum(), 0, 5));

        for (String medio : List.of("Efectivo", "Tarjeta", "Transferencias")) {
            Row row = sheet.createRow(rowNum++);
            crearCelda(row, 0, "", styles.get("totalLabel"));
            crearCelda(row, 1, "", styles.get("totalLabel"));
            crearCelda(row, 2, "", styles.get("totalLabel"));
            crearCelda(row, 3, "", styles.get("totalLabel"));
            crearCelda(row, 4, "TOTAL GRAL " + medio.toUpperCase() + " ->", styles.get("totalLabel"));
            crearCeldaMoneda(row, 5, safe(totalesPorMedio.get(medio)), styles.get("totalMoney"));
        }

        Row total = sheet.createRow(rowNum++);
        crearCelda(total, 0, "", styles.get("grandTotalLabel"));
        crearCelda(total, 1, "", styles.get("grandTotalLabel"));
        crearCelda(total, 2, "", styles.get("grandTotalLabel"));
        crearCelda(total, 3, "", styles.get("grandTotalLabel"));
        crearCelda(total, 4, "TOTAL GENERAL ->", styles.get("grandTotalLabel"));
        crearCeldaMoneda(total, 5, totalGeneral, styles.get("grandTotalMoney"));

        return rowNum;
    }

    private void configurarHoja(Sheet sheet) {
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);

        sheet.setMargin(Sheet.LeftMargin, 0.2);
        sheet.setMargin(Sheet.RightMargin, 0.2);
        sheet.setMargin(Sheet.TopMargin, 0.3);
        sheet.setMargin(Sheet.BottomMargin, 0.3);

        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);
    }

    private void ajustarColumnas(Sheet sheet) {
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        sheet.setColumnWidth(2, 22 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(4, 18 * 256);
        sheet.setColumnWidth(5, 16 * 256);
    }

    private Map<String, CellStyle> crearStyles(Workbook workbook) {
        Map<String, CellStyle> map = new HashMap<>();

        DataFormat format = workbook.createDataFormat();

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        Font normalFont = workbook.createFont();
        normalFont.setFontHeightInPoints((short) 10);

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        map.put("title", title);

        CellStyle subtitle = workbook.createCellStyle();
        subtitle.setFont(boldFont);
        subtitle.setAlignment(HorizontalAlignment.LEFT);
        map.put("subtitle", subtitle);

        CellStyle subtitleRight = workbook.createCellStyle();
        subtitleRight.setFont(boldFont);
        subtitleRight.setAlignment(HorizontalAlignment.RIGHT);
        map.put("subtitleRight", subtitleRight);

        CellStyle filter = workbook.createCellStyle();
        filter.setFont(normalFont);
        filter.setAlignment(HorizontalAlignment.LEFT);
        map.put("filter", filter);

        CellStyle blockTitle = workbook.createCellStyle();
        blockTitle.setFont(boldFont);
        blockTitle.setAlignment(HorizontalAlignment.LEFT);
        setBorders(blockTitle);
        map.put("blockTitle", blockTitle);

        CellStyle header = workbook.createCellStyle();
        header.setFont(boldFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(header);
        map.put("header", header);

        CellStyle cell = workbook.createCellStyle();
        cell.setFont(normalFont);
        cell.setAlignment(HorizontalAlignment.LEFT);
        setBorders(cell);
        map.put("cell", cell);

        CellStyle money = workbook.createCellStyle();
        money.setFont(normalFont);
        money.setAlignment(HorizontalAlignment.RIGHT);
        money.setDataFormat(format.getFormat("$ #,##0.00"));
        setBorders(money);
        map.put("money", money);

        CellStyle subtotalLabel = workbook.createCellStyle();
        subtotalLabel.setFont(boldFont);
        subtotalLabel.setAlignment(HorizontalAlignment.RIGHT);
        setBorders(subtotalLabel);
        map.put("subtotalLabel", subtotalLabel);

        CellStyle subtotalMoney = workbook.createCellStyle();
        subtotalMoney.setFont(boldFont);
        subtotalMoney.setAlignment(HorizontalAlignment.RIGHT);
        subtotalMoney.setDataFormat(format.getFormat("$ #,##0.00"));
        setBorders(subtotalMoney);
        map.put("subtotalMoney", subtotalMoney);

        CellStyle totalLabel = workbook.createCellStyle();
        totalLabel.setFont(boldFont);
        totalLabel.setAlignment(HorizontalAlignment.RIGHT);
        totalLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(totalLabel);
        map.put("totalLabel", totalLabel);

        CellStyle totalMoney = workbook.createCellStyle();
        totalMoney.setFont(boldFont);
        totalMoney.setAlignment(HorizontalAlignment.RIGHT);
        totalMoney.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalMoney.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalMoney.setDataFormat(format.getFormat("$ #,##0.00"));
        setBorders(totalMoney);
        map.put("totalMoney", totalMoney);

        CellStyle grandTotalLabel = workbook.createCellStyle();
        grandTotalLabel.setFont(boldFont);
        grandTotalLabel.setAlignment(HorizontalAlignment.RIGHT);
        grandTotalLabel.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        grandTotalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(grandTotalLabel);
        map.put("grandTotalLabel", grandTotalLabel);

        CellStyle grandTotalMoney = workbook.createCellStyle();
        grandTotalMoney.setFont(boldFont);
        grandTotalMoney.setAlignment(HorizontalAlignment.RIGHT);
        grandTotalMoney.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        grandTotalMoney.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        grandTotalMoney.setDataFormat(format.getFormat("$ #,##0.00"));
        setBorders(grandTotalMoney);
        map.put("grandTotalMoney", grandTotalMoney);

        return map;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void crearCelda(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void crearCeldaMoneda(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(safe(value).doubleValue());
        cell.setCellStyle(style);
    }

    private String mapMedioPago(MedioPago medio) {
        if (medio == null)
            return "Efectivo";

        return switch (medio) {
            case EFECTIVO -> "Efectivo";
            case TARJETAS -> "Tarjeta";
            case TRANSFERENCIA -> "Transferencias";
        };
    }

    private String mapMedioIngresoManual(MedioIngresoManual medio) {
        if (medio == null)
            return "Efectivo";

        return switch (medio) {
            case EFECTIVO -> "Efectivo";
            case BANCO -> "Transferencias";
        };
    }

    private String mapCategoriaIngresoManual(CategoriaIngresoManual categoria) {
        if (categoria == null)
            return null;

        return switch (categoria) {
            case CUOTAS_ATRASADAS -> "RECARGOS";
            case FUTBOL_ALQUILERES -> "ALQUILER";
            case FUTBOL_TORNEOS -> "FUTBOL";
            default -> null;
        };
    }

    private String normalizarDisciplina(String nombre) {
        if (nombre == null)
            return "";

        return nombre.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Getter
    @AllArgsConstructor
    private static class MovimientoExcel {
        private String bloque;
        private String sede;
        private String centroCosto;
        private String rubro;
        private String formaPago;
        private String medioPago;
        private BigDecimal total;
    }

    @Getter
    @AllArgsConstructor
    private static class ResultadoBloque {
        private int siguienteFila;
        private BigDecimal totalBloque;
        private Map<String, BigDecimal> totalesPorMedio;
    }
}