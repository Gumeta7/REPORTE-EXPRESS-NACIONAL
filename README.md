# Reportes Express 📱⚙️

**Reportes Express** es una aplicación móvil nativa para Android desarrollada con **Kotlin** y **Jetpack Compose**, diseñada para agilizar la gestión de reportes de fallas técnicas, la consulta de ubicación en sala de máquinas recreativas/terminales y la comunicación directa con proveedores técnicos y de soporte.

---

## 🚀 Características Principales

### 1. ⚡ Generador de Reportes Rápidos
- **Borradores Automatizados**: Genera mensajes de reporte formateados con saludos según el horario del día (*Buenos días*, *Buenas tardes*, *Buenas noches*).
- **Envío Multicanal**: Integración directa con clientes de correo instalados en el dispositivo (Gmail, Outlook, etc.).
- **Gestión Inteligente de Destinatarios**:
  - Asociación automática de correo según la marca o proveedor de la máquina.
  - Alerta interactiva cuando un proveedor no tiene correo asignado, con la opción de registrarlo al instante o continuar con el campo en blanco.

### 2. 📂 Extraer Datos de Archivos
- Importación y análisis inteligente de archivos de texto, CSV y Excel (`.xlsx`, `.xls`).
- Extracción automática de información relevante de fallas para autocompletar reportes.

### 3. 📍 ¿Dónde está la Máquina? (Catálogo y Ubicación)
- **Carga Masiva vía Excel/CSV**: Importación rápida de inventarios completos de máquinas.
- **Identificación MODELO REPORTE (PP / PV)**:
  - Detección automática de columnas `MODELO REPORTE`.
  - Clasificación visual e insignias distintivas para máquinas de propiedad propia `(PP)` en verde y de proveedor `(PV)` en morado.
- **Búsqueda Instantánea**: Filtrado dinámico por número de máquina, asset, serie, juego, área, isla o marca.
- **Acceso Directo a Falla**: Reporte inmediato al seleccionar cualquier máquina del catálogo.

### 4. 📋 Gestión de Visitas y Proveedores
- Registro y seguimiento de visitas técnicas a salas.
- **Catálogo de Proveedores**: Administración de correos y nombres de proveedores con soporte para registros sin e-mail obligatorio.

### 5. 📜 Historial de Reportes
- Registro histórico con estatus de reportes generados y copias de los mensajes enviados.

---

## 🎨 Experiencia de Usuario y Diseño

- **Navegación Fluida**: Transición por desplazamiento horizontal (*HorizontalPager*) entre pestañas (**Generar**, **Extraer**, **Ubicación**, **Visitas**, **Historial**).
- **Diseño Moderno M3**: Construido sobre las guías de **Material Design 3**, con soporte para modo claro y modo oscuro.
- **Adaptabilidad**: Soporte completo para diferentes tamaños de pantalla e integración edge-to-edge.

---

## 🛠️ Arquitectura y Tecnologías

- **Lenguaje**: Kotlin 100%
- **UI Framework**: Jetpack Compose con Material Design 3
- **Navegación**: Jetpack Compose Navigation & HorizontalPager
- **Persistencia Local**: Room Database + SQLite
- **Asincronía**: Kotlin Coroutines & StateFlow
- **Procesamiento de Archivos**: Apache POI (Excel) & CSV Parsers Custom
- **Inyección de Dependencias**: Constructor Injection & ViewModel Factory

---

## 📦 Estructura del Proyecto

```
app/src/main/java/com/example/
├── data/
│   ├── db/              # Entidades Room (MachineEntity, ReportEntity, ProviderEmailEntity)
│   ├── repository/      # Repositorio de datos unificado
│   └── demo/            # Datos iniciales para pruebas
├── ui/
│   ├── components/      # Diálogos y componentes reutilizables (Alertas de correo, vistas previas)
│   ├── screens/         # Pantallas principales (QuickReport, ExtractFile, MachineLocation, Visits, History)
│   ├── theme/           # Configuración de colores, tipografía y tema Material 3
│   └── viewmodel/       # ReportViewModel para manejo de estado
├── util/                # Utilidades de lectura de Excel/CSV y formato
└── MainActivity.kt      # Contenedor principal con Pager y barra de navegación
```

---

## ⚙️ Requisitos de Compilación

- **Android Studio**: Ladybug / Jellyfish o superior
- **Gradle**: 8.x
- **SDK Mínimo**: Android 7.0 (API level 24)
- **SDK Objetivo**: Android 14 (API level 34)
- **JDK**: Java 17

---

## 📝 Licencia

Este proyecto está disponible para uso interno y personal.
