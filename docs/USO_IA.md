# Registro del trabajo asistido por inteligencia artificial

Este documento describe el trabajo realizado sobre los archivos reales del proyecto DUNAB y distingue las comprobaciones ejecutadas de las revisiones que debe hacer el equipo. No atribuye al estudiante ni al equipo pruebas manuales que no se hayan realizado.

## Material inspeccionado

Se revisó la estructura inicial de la carpeta: `MenuDUNAB (1).java`, `Interfaces_DUNAB (1).pdf` y `.vscode/settings.json`. El código recibido declaraba el paquete `javaapplication1` y la clase pública `MenuDUNAB`, con un menú Swing que usaba bloques generados por NetBeans y varios botones informativos.

No se encontró `MenuDUNAB.form`, una configuración `nbproject`, un archivo de compilación ni una versión de Java declarada. Se creó `respaldo/MenuDUNAB.original.java.txt` antes de integrar cambios importantes; el archivo original de la raíz también se conservó.

**Nota sobre las rutas históricas:** las ubicaciones de originales, PDF, `.vscode`, respaldo, scripts Python y resultados temporales mencionadas en este registro corresponden al momento de cada trabajo. Después se trasladaron a la carpeta hermana [DUNAB_material_aparte](../../DUNAB_material_aparte/), conservando sus rutas relativas. Por ejemplo, la evidencia histórica `tmp/vistas` está archivada bajo `DUNAB_material_aparte/tmp/vistas`. Las pruebas pueden generar nuevas vistas en el `tmp/vistas` del proyecto. `tmp/jdk` permanece dentro del proyecto por su uso en `tools/build.ps1`.

## Código desarrollado con ayuda de IA

| Archivo en `src/javaapplication1/` | Desarrollo realizado |
| --- | --- |
| `MenuDUNAB.java` | Integración del menú existente como ventana principal, pantalla de acceso, navegación por rol y selección de semestre, fuera de los bloques generados del diseñador. |
| `PanelAcceso.java` | Registro local, confirmación de contraseña, inicio de sesión y presentación de cuentas ficticias. |
| `Usuario.java` | Modelo encapsulado de cuenta, rol, información académica, meta global y credenciales derivadas. |
| `Actividad.java` | Modelo de actividad con fecha, lugar, cupos, categoría, reconocimiento y estado. |
| `Inscripcion.java` | Modelo de inscripción y su cancelación conservando el registro. |
| `SolicitudParticipacion.java` | Solicitud, evidencia, cantidad, semestre, estado y observación de revisión. |
| `MovimientoDunab.java` | Registro de aprobaciones y ajustes que permiten recalcular el saldo. |
| `Meta.java` | Objetivo personal por categoría y período, con progreso calculado desde movimientos. |
| `RegistroAuditoria.java` | Constancia de operaciones administrativas, autor responsable, motivo y saldos anterior y posterior. |
| `DatosSistema.java` | Contenedor serializable con las colecciones de información. |
| `Persistencia.java` | Lectura, escritura temporal y reemplazo del archivo local; mensajes de error que conservan el archivo existente. |
| `SeguridadClave.java` | PBKDF2 con sal aleatoria y comparación de la clave derivada. |
| `ServicioDunab.java` | Autenticación, permisos, validaciones, inscripciones, solicitudes, revisión, ajustes, ranking, metas, saldos y datos iniciales ficticios. |
| `Ui.java` | Estilos comunes, componentes de formulario, tablas y presentación de errores. |
| `PantallasEstudiante.java` | Dashboard, perfil, consulta, catálogo, inscripción, participación, historial, metas, ranking, calculadora y estadísticas. |
| `PantallasAdmin.java` | Gestión de actividades, revisión con evidencia, ajustes justificados y consulta de auditoría. |
| `CalculadoraNotas.java` | Cálculo independiente de notas, validación de rangos y acceso encapsulado a sus valores. |

La separación en modelos, servicio, almacenamiento y pantallas evita reunir toda la aplicación en `MenuDUNAB`. Se conservaron los nombres del paquete original y la clase central solicitada.

## Configuración, pruebas y documentación

Se desarrollaron estos archivos complementarios:

- `build.xml`, `nbproject/project.xml`, `nbproject/project.properties` y `nbproject/build-impl.xml`: proyecto Java con Ant, fuentes en `src`, pruebas en `test`, codificación UTF-8 y clase principal `javaapplication1.MenuDUNAB`.
- `manifest.mf`: punto de entrada para el JAR ejecutable.
- `tools/build.ps1`: compilación y generación del JAR; opciones para pruebas, ejecución y vistas previas, con detección de JDK local.
- `test/javaapplication1/PruebasAdmin.java`: pruebas de autorización, cupos, cancelación, rechazo, ajustes, auditoría y recarga de datos.
- `test/javaapplication1/PruebasDunab.java`: escenarios funcionales y de persistencia de la aplicación.
- `test/javaapplication1/PruebasPersistencia.java`: primera ejecución, ausencia de contraseñas en texto plano, corrupción de archivo, fallo real de escritura y recuperación sin cambios parciales.
- `test/javaapplication1/VistaPrevia.java`: generación de vistas de paneles Swing para inspección sin abrir ventanas.
- `test/javaapplication1/PruebasNavegacion.java`: comprobación automática del JFrame, inicio/cierre de sesión, botones de navegación y selector de semestre en un escritorio gráfico, sin mostrar ventanas.
- `tools/extraer_referencias.py`: inspección auxiliar de las páginas y referencias del PDF.
- `tools/integrar_menu.py`: herramienta auxiliar utilizada para integrar el menú preservando las secciones generadas. No se necesita ejecutarla para usar el programa.
- `docs/CONTRATO.md`: contrato de nombres y operaciones usado para coordinar la implementación de los módulos.
- `README.md`, `.gitignore` y este documento: ejecución, modelos, diez funcionalidades, persistencia, verificación y preparación para GitHub.

Los archivos originales y el PDF se conservaron. No se generó un `.form` ficticio para simular compatibilidad con el diseñador visual.

## Decisiones técnicas

1. **Java 8 como base.** No había una versión configurada en el material recibido. Se configuró Java 8 por compatibilidad con el menú original y con las herramientas estándar utilizadas. Puede ejecutarse con un JDK posterior compatible.
2. **Swing sin dependencias.** Se utilizaron `JFrame`, `JPanel`, tablas, barras de progreso y diálogos. Las nuevas pantallas son código Swing normal; no dependen del diseñador de formularios.
3. **ArrayList y HashMap.** Las colecciones persistentes usan `ArrayList`; el servicio mantiene un índice `HashMap` de usuarios por ID. Se emplean ciclos, filtros y ordenamientos para obtener consultas y resúmenes.
4. **Modelos encapsulados.** Los atributos son privados. Los registros persistentes exponen getters y se sustituyen al editar mediante operaciones del servicio. Esto evita cambios de la interfaz que omitan validación o guardado. La calculadora muestra también acceso validado a valores mediante métodos de consulta y modificación.
5. **Saldo derivado.** El acumulado se calcula desde movimientos aprobados y ajustes. Inscribirse o enviar una solicitud no crea DUNAB aprobados. Se separan histórico y semestre.
6. **Protección de contraseñas.** Se usa PBKDF2 con HMAC-SHA256, sal individual aleatoria de 16 bytes, 120.000 iteraciones y derivación de 256 bits. Los datos de demostración son ficticios y públicos.
7. **Validación en el servicio.** Los permisos y las reglas se comprueban al ejecutar cada operación, además de presentar navegación distinta para cada rol.
8. **Persistencia local.** Se utiliza serialización estándar en `data/dunab.dat`, con archivo temporal y reemplazo del archivo definitivo. El servicio conserva una copia de las listas del último estado guardado y la restaura ante un fallo de escritura; los objetos de esas listas son inmutables. No se configura una base de datos ni un servidor.
9. **Historial conservado.** Las actividades se desactivan en lugar de eliminarse. Las solicitudes resueltas permanecen guardadas y cada ajuste deja auditoría con justificación.
10. **Demostración única.** Los datos iniciales se crean solamente cuando no existe el archivo de almacenamiento. Sus fechas relativas quedan fijas tras el primer arranque.
11. **Meta de simulación.** Se usa una sola unidad, DUNAB, con meta inicial personal de 10.000. No se realiza conversión a dinero ni a las 96 horas de una referencia anterior.
12. **Reportes explicables.** Los promedios indican el intervalo y el denominador empleados. No se inventa una fecha de inicio de semestre ni un calendario universitario oficial.

## Comprobaciones de la entrega inicial

Se localizaron inicialmente problemas de entorno: `java` y `javac` no estaban disponibles en `PATH`. Se preparó un JDK 8 portable de desarrollo en la carpeta temporal del proyecto. Esa carpeta está excluida de Git.

Se compiló el código de modelos, persistencia, servicio y pantallas con `javac` de Java 8. Se compiló y ejecutó **`PruebasAdmin`: 44 comprobaciones correctas** en un almacén temporal independiente, sin modificar datos de la aplicación.

Esa suite verificó:

- Denegación de acceso administrativo a estudiantes y denegación de consulta de perfiles o saldos privados ajenos.
- Reserva del último cupo, rechazo de inscripción duplicada o sin capacidad y liberación del cupo al cancelar.
- Solicitud pendiente sin aumento del saldo; rechazo administrativo sin reconocimiento de DUNAB y conservación de su observación.
- Desactivación de actividad con conservación de datos e inscripciones históricas.
- Ajustes positivos y negativos, rechazo de retiros excesivos y de descuentos en un semestre sin saldo.
- Rechazo de ajustes sin motivo o con cantidad cero; separación del saldo por semestre y suma histórica.
- Auditoría de cada ajuste exitoso, con administrador, estudiante, fecha, cantidad, motivo y valores anterior y posterior.
- Recarga de saldos, auditoría y desactivación desde el archivo guardado; cierre de sesión y denegación posterior de administración.

La verificación de la entrega inicial se ejecutó con `powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Test -Preview`. El resultado fue compilación correcta, generación de `dist/DUNAB.jar`, 57 comprobaciones de `PruebasDunab`, 44 administrativas y 16 de persistencia. Esta última suite provocó un fallo real de escritura, verificó el retorno al último estado guardado y confirmó que archivos corruptos o de otro formato se conservan sin sobrescribirlos.

También se compiló y ejecutó `PruebasNavegacion` con `-Djava.awt.headless=false`: **28 comprobaciones correctas** sobre el JFrame real, mediante pulsaciones programáticas de sus botones, selector de semestre, cierre de sesión y cambio de rol. Se utilizaron exclusivamente datos temporales; no se mostró una ventana al usuario ni se creó el almacén `data` del proyecto. El total es **145 comprobaciones automáticas correctas**. Los comandos reproducibles son `tools/build.ps1 -Test`, `tools/build.ps1 -Preview` y, en un equipo con escritorio gráfico, `tools/build.ps1 -GuiTest`.

Se descargó Ant 1.10.15 únicamente dentro de `tmp` y se verificó una compilación independiente de los **17 fuentes Java**, con salida de clases y JAR en carpetas temporales: `BUILD SUCCESSFUL`. Para ejecutar Ant directamente con el Java 8 portable se incluyó su `lib/tools.jar` en el classpath; ese ajuste corresponde al lanzamiento manual de Ant, no a una dependencia de DUNAB. No se ha instalado Ant ni el JDK globalmente en Windows.

`VistaPrevia` generó **17 paneles** sin excepciones y la prueba de navegación generó una captura adicional del menú integrado. Se inspeccionaron el acceso, dashboard, perfil, consulta, catálogo, inscripciones, participación, historial, metas, ranking, calculadora, estadísticas y las pantallas administrativas. La revisión detectó y corrigió la alineación horizontal de formularios y un texto blanco invisible sobre el fondo pintado por Nimbus en el acceso. Se ajustó el color de las barras a morado. Las vistas están en `tmp/vistas`, excluidas de Git. Las tablas anchas permiten redimensionar columnas o desplazarse para leer todo su contenido.

Se compararon automáticamente los bloques `GEN-BEGIN:initComponents` y `GEN-BEGIN:variables` del menú integrado con el original: **idénticos**. Los XML de compilación y proyecto se pudieron analizar correctamente. El archivo original y su respaldo se conservaron.

## Actualización de tema, semestres y calculadora

Después de la entrega inicial se solicitó ampliar los semestres, habilitar el modo oscuro, permitir otras cantidades de notas y porcentajes y aclarar el recorrido para registrar una participación. Se realizaron estos cambios:

- Se añadió `src/javaapplication1/Semestres.java`, con el rango 1 a 12 centralizado en `MAXIMO`. Se actualizaron validaciones del servicio, registro, perfil, selectores, filtros, metas y ajustes. Los modelos serializados conservaron sus campos e identificadores de versión para mantener los datos existentes.
- Se añadió `src/javaapplication1/Tema.java`, con tema claro y oscuro, controles de cambio y preferencia en `data/apariencia.properties`. Se integró con `Ui`, el acceso y la ventana principal. El cambio aplica colores a los componentes existentes, conservando los campos escritos y las selecciones; no reconstruye el formulario.
- Se amplió `CalculadoraNotas.java` con ponderación de arreglos de notas y porcentajes, nota necesaria de una evaluación y distribución de 100 % entre 1 y 30 evaluaciones. Se conservaron los métodos de tres cortes para compatibilidad con el ejemplo inicial.
- Se actualizó `PantallasEstudiante.java` con una tabla editable de evaluaciones, cantidad configurable, reparto de pesos, total visible y validación de suma de 100 %. El resultado se invalida al editar los datos. Se admite dejar vacía solamente la nota pendiente seleccionada al despejarla.
- Se aclaró el formulario de participación y se separaron las reglas de elegibilidad del reconocimiento: inscripción activa, fecha de hoy o anterior y ausencia de otra solicitud pendiente o aprobada. La actualización posterior descrita más abajo aplica esas reglas al botón de envío y conserva visibles todas las inscripciones activas.
- Se añadieron `PruebasSemestres.java`, `PruebasCalculadora.java` y `PruebasTema.java`; se amplió el recorrido de navegación y se incorporaron las nuevas suites al script de compilación.
- Se actualizaron `README.md` y esta documentación y se creó [CAMBIOS_Y_GUIA.md](CAMBIOS_Y_GUIA.md), con instrucciones de los nuevos controles, explicación del selector inicial de dos semestres y un ejemplo de inscripción, solicitud y aprobación.

Se confirmaron mediante ejecuciones independientes con Java 8 estos resultados de las suites nuevas:

| Suite | Comprobaciones correctas | Alcance ejecutado |
| --- | --- | --- |
| `PruebasSemestres` | 58 | Registro y perfil en períodos avanzados, validaciones, solicitudes, ajustes, metas, ranking, recarga y conservación de los semestres 1 y 2. |
| `PruebasCalculadora` | 568 | Notas y pesos configurables, distribución de porcentajes, cálculo ponderado y nota necesaria, límites y validaciones. |
| `PruebasTema` | 46 | Colores de campos, tablas y encabezados; cambio de tema durante edición sin perder texto; conservación de contraseña, selección y datos; preferencia guardada y recargada, modo sin persistencia, fallo de escritura y archivo de apariencia inválido conservado con retorno a modo claro. |

En las pruebas de tema se comparó el archivo serializado de DUNAB antes y después de los cambios visuales: permaneció idéntico y pudo reabrirse. Todas estas pruebas utilizaron carpetas temporales separadas. Se detectó que `Properties.load` también puede lanzar `IllegalArgumentException` al encontrar un escape inválido; se amplió su manejo para iniciar en modo claro sin sobrescribir el archivo defectuoso y se añadieron dos comprobaciones específicas, pasando de 44 a 46 en esa suite.

La corrida conjunta de regresión confirmó nuevamente 57 comprobaciones funcionales, 44 administrativas y 16 de persistencia, además de las 58 de semestres y 568 de calculadora. La suite de tema ampliada se compiló y ejecutó con **46 comprobaciones correctas**. La navegación de la ventana real se amplió y ejecutó con **43 comprobaciones correctas**, incluyendo el nuevo rango de semestres y los controles de apariencia. El total de las siete suites de la actualización es **832 comprobaciones correctas**. El código actual incluye 19 archivos fuente Java. No se ha ejecutado una sesión interactiva de NetBeans.

Al cerrar la actualización se recompilaron los **19 archivos fuente actuales** y se regeneró `dist/DUNAB.jar`. Se repitieron sobre esas fuentes las **568 comprobaciones de calculadora**, las **46 de tema** y las **43 de navegación**, todas correctas. `VistaPrevia` generó sin errores **33 paneles: 17 claros y 16 oscuros**.

Se inspeccionó una selección de capturas claras y oscuras del acceso, dashboard, calculadora, catálogo y administración. Se ajustaron la distribución de la tabla y la visibilidad de los estados de las actividades. No se afirma que cada una de las 33 imágenes se revisara individualmente. El SHA-256 del archivo `data/dunab.dat` existente fue idéntico antes y después de la verificación, por lo que las comprobaciones no alteraron sus registros.

Se leyeron las preferencias locales de proyectos abiertos de **NetBeans 31** y se verificó que su entrada `openProjectsURLs.0` corresponde a esta carpeta de trabajo. Esa comprobación de ruta no equivale a ejecutar las pruebas desde el IDE. Para cargar la versión corregida en este equipo basta con cerrar la ejecución anterior, limpiar y construir y ejecutar el proyecto con F6, conservando `data`.

## Ajustes posteriores de acceso, nombres e inscripciones

Se aclaró la entrada administrativa con el recorrido **Cerrar sesión → `admin@demo.local` / `Admin123!` → Iniciar sesión**. El registro continúa creando únicamente estudiantes; la cuenta administrativa local se conserva y no requiere un registro nuevo.

Se modificaron los datos iniciales y se añadió una actualización compatible para archivos existentes en `ServicioDunab` y `Persistencia`. La actualización retira los marcadores de los nombres, lugares y conceptos asociados a aprobaciones y conserva los identificadores, fechas, cantidades, estados, credenciales y relaciones entre registros. Los motivos libres de ajustes y la auditoría histórica se conservan literalmente. Antes de reemplazar un archivo antiguo se guarda una copia `dunab-antes-nombres-*.bak` en `data`.

Se cambió el catálogo para presentar **Inscribirme** en la parte superior, seleccionar inicialmente una actividad disponible y abrir **Inscripciones** después de guardar y confirmar la inscripción. **Registrar participación** se trasladó del menú lateral a un botón dentro de **Inscripciones**, manteniendo la función de enviar evidencia para revisión. Los formularios utilizan una acción de navegación compartida para pasar entre estas pantallas.

La revisión del código identificó una causa posible de que el botón inferior no fuera visible: la ventana se fijaba en 840 píxeles de alto y podía superar el área útil de una pantalla de 768 píxeles. Además, un desplazamiento exterior se sumaba al de tablas y formularios. Se ajustó el tamaño de la ventana al área útil del escritorio y se simplificó la disposición central para que cada pantalla gestione su propio desplazamiento. La hipótesis sobre la ventana se obtuvo de la lectura del código; no se atribuye al usuario una resolución de pantalla que no se haya confirmado.

`README.md` y `CAMBIOS_Y_GUIA.md` se actualizaron para explicar el acceso, el nuevo botón y el recorrido **Actividades y espacios → Inscribirme → Inscripciones → Registrar participación → revisión administrativa**. Las cifras de 832 comprobaciones indicadas en la sección anterior corresponden a la actualización previa de tema, semestres y calculadora.

La verificación final de estos ajustes ejecutó `tools/build.ps1 -Test -Preview` y confirmó estas comprobaciones:

| Suite | Comprobaciones correctas |
| --- | --- |
| `PruebasDunab` | 57 |
| `PruebasAdmin` | 44 |
| `PruebasPersistencia` | 16 |
| `PruebasSemestres` | 58 |
| `PruebasCalculadora` | 568 |
| `PruebasTema` | 46 |
| `PruebasNombres` | 51 |
| `PruebasNavegacion` | 60 |
| **Total de esa verificación** | **900** |

`PruebasNombres` verificó la limpieza y la copia previa con archivos temporales que representaban la versión anterior. Comprobó que se conservan identificadores, saldos, credenciales protegidas, fechas, ocupación de cupos, inscripciones, solicitudes y auditoría, y que volver a abrir datos ya normalizados no repite la actualización ni sus respaldos. No se utilizó para modificar el archivo real del estudiante: la limpieza de sus datos existentes se realizará al reabrir la aplicación, con copia automática previa cuando corresponda.

La navegación gráfica de la compilación final ejecutó el recorrido mediante `doClick()` sobre los botones reales: cuenta nueva de estudiante, acceso desde Inicio al catálogo, selección de actividad, **Inscribirme**, consulta automática de Inscripciones, **Registrar participación**, envío con evidencia, comprobación de solicitud pendiente y saldo cero, cambio a administrador, aprobación con observación, vuelta al estudiante y saldo de **125 DUNAB**, conservado al reabrir el almacenamiento temporal. También verificó que **Registrar participación** no está en el menú lateral, que los nuevos accesos funcionan y que **Inscribirme** resulta visible con una ventana de 700 píxeles de alto.

La ventana principal de esa prueba se mantuvo oculta. Sus diálogos `JOptionPane` fueron respondidos automáticamente con un temporizador limitado a los diálogos del proceso de prueba; podían aparecer durante la ejecución. No se afirma que toda la prueba se ejecutara sin ventanas visibles. Los datos y preferencias de la prueba fueron independientes de los del usuario.

Se regeneró el JAR y la compilación adicional con Ant finalizó con **BUILD SUCCESSFUL**. Se generaron sin errores las **33 vistas previas** y se inspeccionó una selección final del inicio, catálogo, Inscripciones y menú integrado en los temas correspondientes. Se compactaron filtros y descripciones y se ajustaron las ayudas a dos filas para evitar recortes. Se volvió a confirmar que los bloques protegidos del menú son idénticos al original. Durante la compilación se corrigió un error de escritura en una cadena de `VistaPrevia`; no corresponde a una modificación del diseño protegido.

No se realizó una sesión interactiva de NetBeans ni se editaron sus formularios mediante Design. La comprobación personal pendiente es cerrar la ejecución anterior, limpiar y construir y ejecutar F6 en el IDE con los archivos corregidos, conservando la carpeta `data`.

## Corrección del selector de participación

Se revisó una nueva incidencia en la que el selector parecía vacío después de inscribirse. La lectura directa del archivo con `ObjectInputStream`, sin instanciar `ServicioDunab` ni escribir datos, confirmó la causa el **18 de noviembre de 2026**: el archivo completo contenía cuatro inscripciones activas de dos cuentas diferentes. Las tres iniciales —dos aprobadas y una pendiente— correspondían a la cuenta de prueba. **Taller de pintura** pertenecía a otro propietario, sin solicitud y con fecha **20 de noviembre de 2026**. El selector anterior podía quedar vacío en ambas cuentas por los estados o por la fecha futura; las inscripciones sí estaban guardadas. La cifra de cuatro corresponde al archivo completo, no a un mismo estudiante. El diagnóstico no modificó registros ni datos personales.

Se cambió la presentación para mantener **todas las inscripciones activas visibles y seleccionables**, con fecha y estado, incluidas actividades futuras y solicitudes pendientes o aprobadas. La elegibilidad se aplica a **Enviar solicitud de reconocimiento**: solo se permite enviar si la fecha ya llegó y no existe otra solicitud pendiente o aprobada. Las inscripciones canceladas no se presentan como activas. Un rechazo permite reenviar evidencia corregida cuando la actividad corresponde.

La documentación de uso se actualizó para explicar cada estado y distinguir una lista sin inscripciones activas de un envío temporalmente bloqueado. No se cambian las reglas de reconocimiento ni se suman DUNAB al inscribirse.

La compilación y generación del JAR finalizaron correctamente. Sobre la última fuente se ejecutaron **30 comprobaciones de `PruebasParticipacion`** y **68 de `PruebasNavegacion`**, todas correctas. Esta corrida fue focalizada en la corrección; las 900 comprobaciones de la sección anterior no se reejecutaron como conjunto en esta fase.

Las pruebas comprobaron una inscripción futura visible y seleccionable, la explicación de su fecha y el bloqueo del envío, así como participaciones pendientes y aprobadas visibles sin duplicar solicitudes. También verificaron el recorrido completo de una actividad de hoy, envío de evidencia y aprobación administrativa. No se modificaron los modelos ni las reglas del servicio para permitir participaciones futuras.

Se reprodujo además el caso comunicado sobre una **copia temporal** de `data/dunab.dat`: Taller de pintura pudo seleccionarse, mostró el motivo y la fecha del 20 de noviembre y mantuvo Enviar deshabilitado. El arnés configuró en memoria la sesión de su propietario dentro de la copia, sin solicitar su contraseña; no se modificó el mecanismo de autenticación del producto. Esta reproducción comprueba la presentación de los registros existentes, no un inicio de sesión con las credenciales personales del usuario.

Se generaron los **33 paneles de vista previa** y se inspeccionaron la captura de la inscripción futura en oscuro y `tmp/vistas/39-selector-inscripcion-guardada.png`, correspondiente a la reproducción del caso guardado. El archivo original permaneció intacto: su SHA-256 fue `A031246A556F35500D6E9D5B4A9FA3335D7E62DF126EB94BE3CED97E973ACC08` antes y después del diagnóstico y las pruebas. Para registrar evidencia el mismo día de la revisión se requiere una actividad de ese día; la fecha de Taller de pintura se conserva.

## Límites de la verificación y revisión personal

No se realizó una sesión interactiva de NetBeans ni se comprobó visualmente una pestaña Design. No se afirma que el profesor o el equipo hayan validado el programa. Las vistas generadas sin ventana permiten inspeccionar componentes y distribución, pero no equivalen a pulsar controles en una aplicación abierta.

El equipo debe comprobar personalmente:

1. Abrir esta carpeta como proyecto en NetBeans, confirmar su plataforma Java y ejecutar **Limpiar y construir** y **Ejecutar proyecto**.
2. Recorrer los 15 escenarios del README usando las ventanas reales, especialmente cierre y apertura de la aplicación y cambio entre dos estudiantes.
3. Confirmar que no aparecen controles administrativos en la sesión de estudiante y que los datos del usuario anterior no permanecen visibles al cambiar de sesión.
4. Revisar ventanas, tablas, desplazamiento, tipografía, tildes y contraste con la escala de pantalla utilizada en la presentación.
5. Revisar el diseño junto al [PDF archivado](<../../DUNAB_material_aparte/Interfaces_DUNAB (1).pdf>) y decidir si desean ajustar detalles visuales desde código Swing. El `.form` original tendría que aportarse si se pretende recuperar su edición en Design.
6. Comprobar las reglas académicas elegidas para la demostración: categorías, DUNAB de cada actividad, meta personal y criterio de evidencia. Son reglas del prototipo y datos ficticios.
7. Confirmar que las cuentas, actividades, evidencias y archivos elegidos para entregar no contienen datos reales de estudiantes.
8. Revisar los archivos que se incluirán en GitHub antes de autorizar una publicación.

## Problemas encontrados y tratamiento

| Situación | Tratamiento |
| --- | --- |
| Faltaban metadatos del proyecto y una versión Java declarada. | Se añadieron configuración Java con Ant e instrucciones de ejecución; Java 8 se declaró como base. |
| El archivo recibido se llamaba `MenuDUNAB (1).java`, aunque declaraba `MenuDUNAB`. | Se conservó el original y se integró la clase compilable con el nombre correcto dentro de `src/javaapplication1`. |
| No se recibió `MenuDUNAB.form`. | Se preservó el código generado existente y se construyeron las pantallas nuevas con Swing normal. No se inventó un formulario. |
| El menú tenía botones informativos. | Se conectaron pantallas con operaciones reales y un servicio compartido de validación y persistencia. |
| No había JDK disponible en PATH al iniciar. | Se utilizó un JDK portable para compilar y ejecutar pruebas; el usuario debe disponer de su JDK al trasladar el proyecto. |
| Las actividades de ejemplo envejecen después del primer arranque. | Se conservan sus fechas para respetar la persistencia; se puede crear una actividad de hoy para repetir la demostración. |
| El almacenamiento contiene toda la simulación local. | Se ignora `data` en Git y se explica cómo conservar una copia cerrando antes la aplicación. |

## Separación conservadora de material auxiliar

Se trasladaron **28 elementos que contienen 321 archivos** a `../DUNAB_material_aparte`, respecto de la raíz del proyecto. Se conservaron sus rutas relativas y no se eliminaron archivos. La carpeta contiene [ARCHIVOS_MOVIDOS.json](../../DUNAB_material_aparte/ARCHIVOS_MOVIDOS.json), con rutas, tamaños y SHA-256, y [LEEME_MATERIAL.md](../../DUNAB_material_aparte/LEEME_MATERIAL.md), con las instrucciones de organización y recuperación.

Se separaron los originales, el PDF, `.vscode`, el respaldo inicial, los scripts `tools/extraer_referencias.py` y `tools/integrar_menu.py` y los resultados o herramientas temporales históricos. Se conservaron el código fuente, las pruebas, documentación, configuración de NetBeans, datos, compilaciones y `tools/build.ps1`, junto con `build.xml`, `manifest.mf`, `.gitignore` y `README.md`. `tmp/jdk` permaneció por ser una alternativa de JDK utilizada por el script de PowerShell.

Durante el traslado se compararon **78 archivos esenciales** mediante SHA-256 y no se detectaron diferencias. La separación no editó código, configuración ni datos. Después se actualizaron únicamente estas referencias de documentación para indicar las ubicaciones actuales; los relatos y resultados de pruebas anteriores se mantienen como historial. Esta comprobación de archivos no equivale a una compilación posterior al traslado.

La verificación posterior al traslado compiló los **19 archivos fuente y 10 archivos de pruebas** y generó un JAR en un directorio temporal externo al proyecto. Las carpetas `build` y `dist` existentes no se reescribieron. Se ejecutaron **`PruebasDunab`: 57 comprobaciones correctas** y **`PruebasParticipacion`: 30 comprobaciones correctas**, para un total de **87 comprobaciones de esas dos suites**. Esta corrida no repitió todas las suites anteriores.

Al finalizar se compararon nuevamente los **78 archivos esenciales**, incluidos los datos y la configuración: sus SHA-256 permanecieron idénticos. El material trasladado sumó **321 archivos y 109,13 MB**, conservados en la carpeta aparte con su manifiesto.

## GitHub y autorizaciones

Se preparó el código para revisión y publicación posterior. **No se publicó ningún repositorio y no se envió ninguna invitación.** La incorporación del profesor **`@juliansant0y0`** como colaborador queda pendiente de la autorización expresa del propietario y de disponer del repositorio elegido.

La documentación contiene únicamente las claves ficticias de demostración. Los archivos locales de datos, compilaciones, herramientas portables y configuración privada de NetBeans están excluidos mediante `.gitignore`.
