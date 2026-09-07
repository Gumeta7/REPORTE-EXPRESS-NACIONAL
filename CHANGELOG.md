# 📋 Registro de Cambios — Versión 1.0.2
**Fecha de lanzamiento:** 24 de Agosto de 2026  
**Identificador de paquete:** `com.winpot.reportesexpress`  
**Versión de compilación:** `v1.0.2` (`versionCode = 2`)  

---

## 🚀 Resumen de la Versión

La versión **1.0.2** introduce una renovación estética y funcional de nivel corporativo (**Enterprise Slate & Cobalt**), navegación fluida por gestos y resortes, integración de logotipos oficiales de salas en alta resolución con compatibilidad para Modo Oscuro/Claro, sincronización a la nueva hoja de cálculo central y soporte condicional para el campo **Título de Juego** en las tarjetas del catálogo.

---

## ✨ Nuevas Funcionalidades y Mejoras

### 🏛️ 1. Identidad Institucional y Logos Oficiales por Sede
- **Logotipos en Alta Definición:** Integración de imágenes vectorizadas/transparentes de alta nitidez para las sedes de casino:
  - 🔴 **WINPOT:** Variantes dedicadas para Modo Oscuro y Modo Claro.
  - 🍷 **CAPRI CASINO:** Variantes dedicadas para Modo Oscuro y Modo Claro.
  - 💎 **DIAMONDS CASINO:** Logotipo institucional adaptativo para ambos modos.
  - 🎲 **VENETO CASINO:** Logotipo institucional de alto contraste para ambos modos.
- **Presentación Limpia y Prominente:** Los logotipos se muestran directamente sobre la barra superior sin recuadros ni tarjetas que reduzcan su tamaño, con optimización automática de bordes transparentes.

---

### 🌟 2. Nueva Barra de Navegación Animada (`AnimatedExpandingBottomBar`)
- **Efecto Expansible Elástico:** La pestaña seleccionada se expande suavemente con física de resortes (`Spring.DampingRatioMediumBouncy`), desplegando una píldora con degradado **Cobalt Blue**.
- **Entrada Fluida de Texto:** Despliegue horizontal sincronizado del nombre de la sección (`Generar`, `Actualizar`, `Máquinas`, `Visitas`, `Historial`) con micro-animaciones en los íconos.
- **Diseño Flotante:** Esquinas redondeadas (`26.dp`) y elevación en Slate 900/800 a 60 FPS estables.

---

### 👤 3. Cabecera Ejecutiva y Perfiles de Usuario
- **Avatar Monograma:** Generación de iniciales con iluminación Cobalt para el técnico activo (`TechnicianMonogramAvatar`).
- **Rol Simplificado:** Ajuste de etiqueta a **`Técnico`**, *`Administrador Corporativo`* o *`Director Corporativo`* según el perfil configurado.
- **Insignia Dinámica:** Muestra automáticamente el logotipo de la sala asignada en la esquina superior derecha.

---

### 🎰 4. Catálogo de Máquinas y Tarjetas de Identificación
- **Campo Condicional de Título:** Se incorporó la barra de **`Título:`** del juego en la tarjeta de máquina. Al igual que el campo *Isla*, **únicamente se muestra si existe información en el Excel o Google Drive**, sin ocupar espacio innecesario si está ausente.
- **Reporte Directo:** Al pulsar `⚡ Reportar Falla de esta Máquina`, se abre de inmediato el diálogo modal para escribir la falla y enviar el reporte.
- **Cuadrícula Simétrica:** Distribución 2x2 para `Asset`, `Área / Isla`, `Marca`, `Modelo` y barra dedicada de `Número de Serie`.

---

### 📋 5. Optimización en Registro de Visitas Técnicas
- **Escritura 24h sin Saltos:** Corrección de cursor en los campos de entrada de hora (`TextFieldValue` con selección fija `TextRange`).
- **Autocompletado de Isla:** Al ingresar el número de `Asset`, autocompleta la Isla real registrada en la base de datos local.
- **Campos Limpios:** El campo de técnico visitante inicia en blanco sin forzar la sala actual.

---

### ☁️ 6. Motor de Sincronización y Fusión No Destructiva
- **Nueva URL de Google Sheets:** Actualizado el enlace oficial de la hoja de cálculo en [`DriveSyncService.kt`](file:///c:/Users/Latitude%205401/Documents/APPNACIONAL/app/src/main/java/com/example/data/remote/DriveSyncService.kt).
- **Fusión No Destructiva (`mergeAndImportMachines`):** Al sincronizar con Google Drive, la app no borra las **Islas** ni los **Títulos** complementados localmente mediante archivos Excel.
- **Parseo Estricto:** Detección precisa de encabezados sin recurrir a posiciones fijas ni datos genéricos por defecto.

---

### 📦 7. Empaquetado y Configuración
- **`applicationId` Oficial:** `com.winpot.reportesexpress`.
- **Compatibilidad de Actualización:** Permite instalar el nuevo APK firmado sobre versiones existentes sin pérdida de base de datos ni reportes locales.
