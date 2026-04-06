# 🏟️ Club Banco - Sistema de Gestión

Sistema web fullstack desarrollado para la administración de un club deportivo. Permite gestionar socios, pagos, ingresos, gastos y visualizar dashboards financieros en tiempo real.

---

## 🚀 Demo

🔗 Frontend: https://club-banco.netlify.app/login  

> ⚠️ Acceso restringido a administradores

---

## 🧰 Tecnologías utilizadas

### Backend

* Java 17
* Spring Boot
* Spring Security (JWT)
* Hibernate / JPA
* PostgreSQL (Supabase)
* Flyway

### Frontend

* Angular 21
* TypeScript
* Bootstrap
* Chart.js

### DevOps

* Netlify (Frontend)
* Render (Backend)
* Supabase (Base de datos)

---

## ⚙️ Funcionalidades principales

### 👤 Gestión de socios

* Alta de socios
* Activar / desactivar
* Asociación a disciplina
* Estado de pago automático (AL DÍA / DEBE)

### 💰 Pagos

* Registro de cuotas mensuales
* Inscripción
* Cálculo automático de montos
* Historial de pagos por socio

### 📊 Dashboard de ingresos

* Ingresos por disciplina
* Filtros por fecha, categoría y búsqueda
* Exportación a Excel

### 📉 Dashboard de gastos

* Registro de gastos
* Clasificación por categorías
* Gráficos dinámicos
* Filtros avanzados

### ⚖️ Balance

* Comparación de ingresos vs gastos
* Vista mensual o por rango de fechas

---

## 🔐 Autenticación

* Login con JWT
* Roles:

  * ADMIN
  * SOCIO (preparado para futuras funcionalidades)

---

## 📦 Arquitectura

Backend con arquitectura en capas:

* Controller
* Service
* Repository
* DTOs

Frontend modular con Angular:

* Features
* Services
* Components standalone

---

## 📸 Capturas

### 🔐 Login
![Login](https://raw.githubusercontent.com/TomasBreppe/Club-Banco/main/FrontEnd/assets/login.png)

---

### 💰 Dashboard de ingresos
![Ingresos](https://raw.githubusercontent.com/TomasBreppe/Club-Banco/main/FrontEnd/assets/ingresos.png)

---

### 👥 Gestión de socios
![Socios](https://raw.githubusercontent.com/TomasBreppe/Club-Banco/main/FrontEnd/assets/socios.png)
---

## 🧠 Autor

Desarrollado por **Tomás Breppe**  
💼 Técnico Universitario en Programación  

---

## 📬 Contacto

* LinkedIn:  
  https://www.linkedin.com/in/tomás-breppe-
