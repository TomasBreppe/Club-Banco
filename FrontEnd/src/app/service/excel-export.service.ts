import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

@Injectable({
  providedIn: 'root',
})
export class ExcelExportService {
  exportToExcel(data: any[], fileName: string, sheetName: string = 'Datos'): void {
    if (!data || data.length === 0) {
      return;
    }

    const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
    const workbook: XLSX.WorkBook = {
      Sheets: { [sheetName]: worksheet },
      SheetNames: [sheetName],
    };

    const excelBuffer: ArrayBuffer = XLSX.write(workbook, {
      bookType: 'xlsx',
      type: 'array',
    });

    const blob = new Blob([excelBuffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8',
    });

    saveAs(blob, `${fileName}.xlsx`);
  }
}