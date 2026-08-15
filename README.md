# Reportes Express 📱⚙️

**Reportes Express** es una aplicación móvil nativa para Android desarrollada con **Kotlin** y **Jetpack Compose (Material Design 3)**, diseñada para la gestión técnica en sala de máquinas de juego, consulta de catálogo por ubicación, control de incidencias, generación automatizada de reportes a proveedores y autenticación sincronizada en la nube con **Google Drive / Google Sheets**.

---

## 🚀 Características Principales

### 1. 🔐 Autenticación y Control de Acceso por Sala
- **Inicio de Sesión Sincronizado**: Las credenciales se extraen y validan directamente de la hoja **`Tecnicos`** del archivo en Google Drive (`Usuario`, `Contraseña`, `Nombre`, `Sala`, `Rol`, `Estatus`).
- **Filtrado Automático por Sala**:
  - **Técnicos de Sala**: Acceso restringido exclusivamente a las máquinas y datos de su sala asignada (ej. *Winpot Puerta de Hierro*, *Winpot Metrocentro*).
  - **Administradores**: Acceso global y sin restricciones para visualizar y gestionar todas las salas y máquinas registradas.
- **Sesión Persistente y Cierre Seguro**: Mantiene la sesión iniciada en el dispositivo y permite cerrar sesión desde el botón de la barra superior.

### 2. ☁️ Sincronización en la Nube (Google Drive)
- **Sincronización Automática al Iniciar**: Descarga y guarda localmente en SQLite/Room el catálogo de máquinas de la hoja **`Maquinas`** y los técnicos de la hoja **`Tecnicos`**.
- **Actualización con un Toque**: Botón para refrescar la información en cualquier momento desde la nube, reemplazando la base de datos local limpiamente sin generar registros duplicados.
- **Privacidad y Consistencia de Datos**:
  - Los técnicos solo ven el conteo de máquinas asignadas a su sala y no visualizan listas de otras salas ni de otros técnicos.
  - Los administradores tienen acceso a la vista general de todas las salas detectadas.
- **Operación Offline**: Permite consultar el catálogo y preparar reportes de incidencias incluso sin conexión a internet activa.

### 3. 📍 Catálogo de Máquinas (7 Campos Principales)
La información de cada máquina se organiza y presenta visualmente en tarjetas de diseño moderno basadas en los siguientes 7 campos:
1. 🏛️ **Sala:** Nombre del casino o complejo asignado.
2. 🏢 **Propietario:** Especificación de la empresa o propietario del equipo.
3. 🏷️ **Marca:** Fabricante de la terminal (ej. *Ainsworth*, *IGT*, *EGT*, *ZITRO*, *Dreidel*).
4. 📐 **Modelo:** Modelo del gabinete o equipo (ej. *A560H*, *PEAK 49*, *VS 600*, *FENIX 43*).
5. 🔢 **Asset:** Número económico o número de inventario.
6. 🔤 **Serie:** Número de serie físico de la máquina.
7. 📍 **Área:** Sector o zona de la sala (ej. *FUMADORES*, *NO FUMAR*, *CALLE*).

- **Búsqueda Instantánea**: Búsqueda dinámica en tiempo real por cualquiera de los 7 campos.
- **Reporte Directo desde Catálogo**: Botón en cada tarjeta para redactar y enviar una incidencia específica con todos los datos precargados.

### 4. ⚡ Generador Rápido de Reportes
- **Borradores Automatizados**: Genera plantillas de correo con saludos según la hora del día (*Buenos días*, *Buenas tardes*, *Buenas noches*).
- **Asociación Inteligente**: Autocompleta automáticamente los 7 campos del equipo y el correo del proveedor según la marca.
- **Alerta de Proveedor sin Correo**: Diálogo interactivo para registrar el correo del proveedor al vuelo o continuar en blanco.
- **Envío Multicanal**: Integración con las aplicaciones de correo del dispositivo (Gmail, Outlook, etc.).

### 5. 📋 Gestión de Visitas Técnicas
- Registro de entradas, salidas y motivos de visita de técnicos y proveedores.
- Autocompletado rápido con las salas sincronizadas.
- Generación de formato de texto listo para compartir por WhatsApp.

### 6. 📜 Historial de Reportes
- Registro histórico de reportes emitidos con fecha, hora, estatus y copia del mensaje enviado.

---

## 🎨 Experiencia de Usuario y Diseño

- **Material Design 3 (M3)**: Interfaz moderna con soporte completo para **Modo Claro** y **Modo Oscuro**.
- **Navegación Fluida**: Transición por desplazamiento horizontal (*HorizontalPager*) entre pestañas (**Generar**, **Actualizar**, **Máquinas**, **Visitas**, **Historial**).
- **Identificación Visual**: Insignia con el nombre del usuario y sala activa en la barra superior.

---

## 🛠️ Arquitectura y Tecnologías

- **Lenguaje**: Kotlin 100%
- **UI Framework**: Jetpack Compose con Material Design 3
- **Persistencia Local**: Room Database 2.7 + SQLite
- **Asincronía**: Kotlin Coroutines & StateFlow
- **Cliente HTTP**: OkHttp 4 (descarga directa de XLSX desde Google Sheets)
- **Procesamiento de Archivos**: Apache POI (Excel) & CSV Parsers Custom
- **Arquitectura**: MVVM (Model-View-ViewModel) + Repository Pattern

---

## 📦 Estructura del Proyecto

```
app/src/main/java/com/example/
├── data/
│   ├── api/             # Modelos de extracción de datos
│   ├── db/              # Room Entities (MachineEntity, TechnicianEntity, EmailReportEntity, ProviderEmailEntity) y DAOs
│   ├── remote/          # DriveSyncService (Cliente de descarga de Google Drive)
│   ├── repository/      # ReportRepository (Acceso unificado a datos locales y remotos)
│   └── demo/            # Datos demo de respaldo
├── ui/
│   ├── components/      # Diálogos reutilizables (DraftPreview, MissingProviderEmail)
│   ├── screens/         # Pantallas (LoginScreen, QuickReport, ExtractFile, MachineLocation, Visits, History)
│   ├── theme/           # Paletas de colores (Claro/Oscuro), tipografía y tema M3
│   └── viewmodel/       # ReportViewModel (Estado global, login, filtros por sala y sincronización)
├── util/                # FileParserUtil (Lector selectivo de hojas 'Maquinas' y 'Tecnicos')
└── MainActivity.kt      # Punto de entrada, control de sesión (Login/App) y Scaffold principal
```

---

## ⚙️ Requisitos de Compilación

- **Android Studio**: Ladybug / Meerkat / Koala o superior
- **Gradle**: 9.3.1 (incluido en Gradle Wrapper)
- **Java / JDK**: Java 21 (LTS)
- **SDK Mínimo**: Android 7.0 (API level 24)
- **SDK Objetivo**: Android 14 (API level 34)

---

## 📝 Licencia

Este proyecto es para uso interno y administrativo.
