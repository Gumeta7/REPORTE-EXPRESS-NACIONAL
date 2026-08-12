# Changelog - REPORTE EXPRESS

Todos los cambios notables realizados en este proyecto se documentan en este archivo.

---

## [Unreleased] - 2026-08-12

### 🚀 Cambios Importantes y Funcionalidad
- **Gestión Anti-Duplicados de Catálogo**:
  - Se agregó un cuadro de diálogo de confirmación al importar un nuevo archivo Excel/CSV si el catálogo local contiene máquinas, permitiendo elegir entre **"Sustituir Base"** (limpieza previa) o **"Añadir a Existente"**.
- **Procesamiento de Archivos Seguro (`Stream Closed` Fix)**:
  - Se optimizó la lectura de hojas de cálculo (`FileParserUtil`) leyendo los bytes del archivo en memoria de forma síncrona antes de pasarlos a hilos secundarios (`Dispatchers.IO`), solucionando definitivamente el fallo `Stream Closed`.
- **Formateador y Validación Estricta de 24 Horas**:
  - Auto-inserción inteligente de dos puntos (`:`) al escribir la hora en los campos de entrada y salida (`HH:mm`).
  - Activación de teclado numérico directo y validación en tiempo real para impedir el ingreso de dígitos fuera del rango horario de 24 horas (00:00 - 23:59).
  - Inicialización limpia sin pre-llenado de horas por defecto.
- **Selector de Fecha con Calendario Material 3**:
  - Integración de `DatePickerDialog` interactivo con corrección de desfase de zona horaria UTC a local.
- **Estabilidad en Compilaciones Release (ProGuard / R8)**:
  - Configuración de reglas de conservación (`proguard-rules.pro`) para Apache POI y bibliotecas XML.

### 🎨 Mejoras de Interfaz
- Rediseño general de la interfaz en las pestañas **Extraer**, **Máquinas** y **Visitas**.
- Adaptación dinámica y paleta de alto contraste para el **Modo Oscuro** en las tarjetas de catálogo y elementos de vista previa.
- Simetría visual y alineación exacta de cajas de texto y botones de acción.
- Animación de etiqueta flotante (*Floating Label*) en el buscador de máquinas.
- Actualización de logotipo a la identidad visual **"G"**.
