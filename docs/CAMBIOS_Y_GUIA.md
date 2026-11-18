# Cambios y guía de uso de DUNAB

## Entrar como administrador

1. Si estás en una cuenta de estudiante, pulsa **Cerrar sesión** en el menú lateral.
2. En la pantalla de acceso escribe **`admin@demo.local`** en Correo y **`Admin123!`** en Contraseña.
3. Pulsa **Iniciar sesión**. El menú mostrará **Administrar actividades**, **Revisar solicitudes**, **Estudiantes y ajustes** y **Auditoría**.
4. Para volver a tu cuenta de estudiante, pulsa **Cerrar sesión** e inicia con el correo y contraseña de esa cuenta.

La cuenta administrativa ya existe entre las cuentas ficticias iniciales. No necesitas crear otra ni seleccionar un rol en el formulario de registro: las cuentas nuevas que se crean allí son de estudiante. Los correos terminados en `@demo.local` y sus contraseñas se conservan aunque los nombres de las actividades y lugares ya no lleven el sufijo `demo`.

Al abrir datos antiguos se retiran los marcadores de los nombres y lugares conservando sus identificadores, inscripciones, saldos y credenciales. Antes de guardar esa limpieza se crea una copia `data/dunab-antes-nombres-*.bak`. No necesitas borrar `data` ni registrar otra vez las cuentas.

## Semestres disponibles

El primer desarrollo ofrecía Semestre 1 y Semestre 2 porque el documento inicial describía expresamente ese selector. Esa elección pertenecía al alcance inicial del prototipo; no significaba que una carrera universitaria tuviera solamente dos semestres.

El rango se amplió a **semestres del 1 al 12**. El valor máximo se centraliza en `Semestres.MAXIMO`, para mantener coherentes el registro, perfil, selector principal, filtros, metas, ranking y ajustes administrativos. El rango sigue siendo una configuración del prototipo, no la duración oficial de todos los programas académicos.

El semestre guardado en el perfil sirve como preferencia al entrar. El selector superior elige el período que estás consultando. Cambiarlo no mueve reconocimientos entre semestres ni modifica el saldo histórico.

Los datos existentes se conservan: las participaciones de demostración de los semestres 1 y 2 siguen donde se registraron. Un semestre sin reconocimientos muestra saldo cero hasta que se apruebe una participación o se registre un ajuste en ese período. La meta global se calcula con el histórico de todos los semestres.

## Modo oscuro

En la pantalla de acceso y dentro de la sesión está disponible el botón **Modo oscuro**. Púlsalo para cambiar el tema; el botón **Modo claro** permite regresar. El cambio afecta a los formularios y a los componentes de consulta, manteniendo el morado como color de navegación.

La elección se guarda localmente en `data/apariencia.properties`. Al volver a abrir la aplicación se recupera esa preferencia. Es una preferencia de esta instalación local, compartida al cambiar de cuenta; no modifica los saldos ni el archivo de movimientos.

## Calculadora con evaluaciones configurables

La calculadora comienza con tres cortes de 30 %, 30 % y 40 % para conservar el ejemplo inicial. Ahora puedes cambiar las notas y los porcentajes, agregar evaluaciones y quitar las que no necesites. Admite de 1 a 30 evaluaciones, con notas entre 0 y 5. Cada porcentaje debe ser mayor que cero y la suma debe ser **100 %**.

1. Cambia **Cantidad de notas (1 a 30)** y pulsa **Aplicar cantidad**. Si necesitas conservar los porcentajes existentes, desmarca antes **Repartir el 100 % al aplicar**; después completa los pesos de las filas añadidas.
2. Edita directamente las columnas **Evaluación**, **Nota (0 a 5)** y **Peso %**. Puedes escribir decimales con punto o coma.
3. Revisa el total de porcentajes que muestra la pantalla. **Repartir 100 %** distribuye el total entre las filas; también puedes escribir una distribución propia.
4. Pulsa **Calcular nota final**. Al modificar cualquier dato, vuelve a calcular para que el resultado corresponda a los valores nuevos.

Aplicar una cantidad mayor añade filas; aplicar una menor quita las últimas. Los nombres y notas de las filas conservadas permanecen. La opción de repartir porcentajes está activada al inicio, para que cambiar el número de evaluaciones produzca inmediatamente una distribución válida.

La nota final se obtiene sumando el aporte de cada evaluación:

```text
Aporte de una evaluación = nota × porcentaje / 100
Nota final = suma de todos los aportes
```

Por ejemplo, cuatro evaluaciones con notas 4, 3, 5 y 4 y pesos 20 %, 25 %, 15 % y 40 % producen:

```text
4 × 0,20 + 3 × 0,25 + 5 × 0,15 + 4 × 0,40 = 3,90
```

Para calcular cuánto necesitas en una evaluación, elige su fila mediante **Fila de evaluación pendiente**, escribe la **Nota mínima deseada** y completa las notas de las demás. Puedes dejar vacía únicamente la nota de esa evaluación pendiente; después pulsa **Calcular nota necesaria**. El cálculo despeja esa evaluación usando su porcentaje. Si el resultado supera 5, no es matemáticamente posible alcanzar la meta con esa distribución; si es menor o igual a cero, el mínimo ya está cubierto por las demás notas. Un porcentaje cero se rechaza; puedes eliminar esa fila o asignarle un porcentaje positivo y ajustar los demás.

Con la plantilla inicial, C1=3, C2=4 y mínimo=3, necesitas 2,25 en el tercer corte. Si además C3=5, la nota final es 4,10. La calculadora es independiente de los DUNAB: sus resultados no suman ni retiran unidades.

## Cómo funciona una inscripción

El reconocimiento tiene tres pasos separados:

```text
Inscripción del estudiante
        ↓ reserva un cupo
Solicitud de participación con evidencia
        ↓ queda pendiente, sin sumar DUNAB
Revisión del administrador
        ↓ si aprueba, incorpora el reconocimiento una sola vez
Consulta del saldo y del historial del estudiante
```

### 1. Inscribirse como estudiante

Entra con una cuenta de estudiante. La cuenta ficticia disponible es:

```text
Correo: estudiante@demo.local
Contraseña: Estudiante123!
```

Abre **Actividades y espacios** desde el menú o pulsa **Ver actividades e inscribirme** en Inicio. El botón **Inscribirme** aparece arriba del catálogo, junto a los controles de búsqueda. La primera actividad disponible se selecciona al cargar; si quieres otra, selecciona su fila y pulsa **Inscribirme**. La actividad debe estar activa, tener cupos y realizarse hoy o en una fecha futura. La columna **Estado** indica si está disponible, finalizada, sin cupos o si ya estás inscrito; el botón se habilita cuando corresponde.

La confirmación significa que se reservó el cupo y se guardó una inscripción real en el almacenamiento local. Después se abre **Inscripciones** para mostrar el registro. El saldo todavía no aumenta. Si intentas reservar otra vez la misma actividad, el programa rechaza la inscripción duplicada. Si otra persona agotó los cupos, informa que ya no hay disponibilidad.

En **Inscripciones**, la columna **Participación** indica el siguiente paso:

| Estado visible | Qué hacer |
| --- | --- |
| Sin solicitud · registra evidencia | Pulsa **Registrar participación** y completa el formulario. |
| Futura · espera su fecha | Conserva tu inscripción y espera a que llegue la actividad. |
| Pendiente de revisión | El administrador debe revisar la solicitud ya enviada. |
| Aprobada · cantidad de DUNAB | El reconocimiento ya está incorporado; puedes consultarlo en el historial. |
| Rechazada · puedes reenviar | Revisa la observación y envía una evidencia corregida. |
| Inscripción cancelada | No tiene un cupo activo para solicitar participación. |

### 2. Solicitar el reconocimiento

Abre **Inscripciones** en el menú lateral y pulsa allí **Registrar participación**. Puedes abrirlo inmediatamente después de inscribirte. Ese botón está dentro de Inscripciones; la opción independiente se retiró del menú lateral para reunir el recorrido en un mismo lugar. El selector **Actividad inscrita · fecha · estado** muestra **todas tus inscripciones activas**, incluidas actividades futuras y participaciones pendientes o aprobadas. Despliega su lista para elegir la que quieras consultar. Al abrir el formulario se selecciona la primera elegible para enviar o, si ninguna lo es, la primera inscripción activa.

Selecciona la actividad para consultar su situación y la **Fecha programada (AAAA-MM-DD)**. El botón **Enviar solicitud de reconocimiento** se habilita cuando esa fecha ya llegó y no existe otra solicitud pendiente o aprobada para ella. Si es elegible, conserva su fecha real, escribe una cantidad positiva que no supere los DUNAB de la actividad e ingresa una evidencia o código de referencia ficticio. Selecciona el semestre donde se reconocerá la participación y envía la solicitud. Cuando no corresponde enviar, los campos permanecen deshabilitados y el mensaje explica el motivo.

La solicitud queda **PENDIENTE** y no aumenta el saldo. El administrador revisará la evidencia. La aplicación no verifica por sí sola una asistencia real ni conecta con servidores universitarios.

El botón **Volver a mis inscripciones** regresa a la lista. Desde allí también puedes usar **Buscar actividades** para abrir el catálogo.

Una actividad futura sigue visible en el selector: selecciónala para conocer su fecha y espera a participar antes de enviar evidencia. Una solicitud pendiente o aprobada también permanece visible, con el motivo que impide otro envío. Después de un rechazo puedes enviar una solicitud corregida cuando la fecha corresponda.

### 3. Aprobar como administrador

Pulsa **Cerrar sesión** para salir de la cuenta del estudiante. En el acceso escribe estos datos y pulsa **Iniciar sesión**:

```text
Correo: admin@demo.local
Contraseña: Admin123!
```

Abre **Revisar solicitudes**, selecciona la fila correspondiente y consulta la evidencia. Pulsa **Aprobar seleccionada**, escribe una observación y confirma. Los DUNAB reconocidos se incorporan al estudiante una sola vez y la operación queda en auditoría.

La opción **Rechazar seleccionada** conserva la solicitud con su observación y no suma DUNAB. Una solicitud revisada no se aprueba otra vez. Para corregir un saldo ya reconocido existe el módulo de ajustes justificados del administrador.

### 4. Verificar el resultado

Cierra la sesión administrativa y vuelve a la cuenta del estudiante. Abre **Consultar DUNAB** y **Historial de DUNAB**. Comprueba el semestre usado en la solicitud y el saldo histórico. Cerrar y volver a abrir la aplicación debe conservar el reconocimiento.

## Cómo interpretar el selector y el botón Enviar

El selector actual muestra todas las inscripciones activas de la cuenta con la que iniciaste sesión. La cuenta inicial de prueba incluye dos participaciones aprobadas y una solicitud pendiente: su propietario puede seleccionarlas para consultar su fecha y estado, pero **Enviar** permanece deshabilitado para evitar duplicarlas. Cada estudiante consulta únicamente sus propias inscripciones.

| Situación de la actividad seleccionada | ¿Se muestra? | ¿Puede enviarse otra solicitud? |
| --- | --- | --- |
| Inscripción activa, actividad de hoy o pasada, sin solicitud | Sí | Sí, con los datos válidos. |
| Inscripción activa, actividad futura | Sí | Cuando llegue su fecha. |
| Solicitud pendiente | Sí | Debe resolverse la solicitud actual. |
| Solicitud aprobada | Sí | Ya tiene reconocimiento; no se duplica. |
| Solicitud rechazada, actividad de hoy o pasada | Sí | Sí, después de corregir la evidencia. |
| Inscripción cancelada | No | Requiere una inscripción activa. |

Una lista vacía significa que esa cuenta no tiene inscripciones activas. Revisa **Inscripciones** y confirma que entraste con la misma cuenta que reservó el cupo. En la versión anterior, el selector solo presentaba actividades listas para un envío nuevo y podía quedar vacío aunque la inscripción estuviera guardada; esa separación invisible se sustituyó por estados explícitos y control del botón Enviar.

En la revisión del **18 de noviembre de 2026** se comprobó el caso que originó la corrección: el archivo completo tenía cuatro inscripciones activas, pertenecientes a dos cuentas diferentes. Las tres iniciales —dos aprobadas y una pendiente— correspondían a la cuenta de prueba. La inscripción en **Taller de pintura** pertenecía a otra cuenta y no tenía solicitud, pero estaba programada para el **20 de noviembre de 2026**. El selector anterior podía quedar vacío para cualquiera de ellas por esos estados o por la fecha futura; la inscripción del taller no se había perdido. Ahora cada cuenta ve sus propias inscripciones activas y su situación. Para Taller de pintura, el envío se habilita desde el 20 de noviembre, después de participar.

Para probar un envío el mismo día de la revisión, el administrador puede crear otra actividad con fecha de hoy; después el estudiante puede inscribirse y recorrer la solicitud y su aprobación.

La actividad **Pausa de bienestar** tiene fecha relativa al primer arranque y ya cuenta con una solicitud pendiente de la cuenta de prueba. Desde esa cuenta se puede seleccionar para consultar su estado y el administrador puede revisar directamente su solicitud, sin volver a registrarla.

Para comprobar el recorrido completo con un envío nuevo, utiliza una de estas opciones:

- **Cuenta nueva:** crea un estudiante de prueba, inscríbelo en una actividad de hoy que tenga cupos y registra su participación.
- **Actividad nueva:** como administrador, crea una actividad distinta con fecha de hoy; después inicia como estudiante, inscríbete y solicita el reconocimiento.

Las fechas de las actividades iniciales se guardan cuando se crea la demostración. No se renuevan al abrir la aplicación otro día, para conservar los datos almacenados.

## Ejemplo completo que puedes repetir hoy

1. Entra como administrador y abre **Administrar actividades → Crear actividad**.
2. Ingresa un nombre nuevo, por ejemplo `Taller de prueba de hoy`; categoría `Cultura`; una descripción; la fecha de hoy con formato `AAAA-MM-DD`; lugar ficticio; 100 DUNAB; 10 cupos y estado activo. Guarda.
3. Cierra sesión y entra con una cuenta de estudiante. Selecciona el semestre que quieras comprobar, por ejemplo 6, y anota su saldo actual.
4. En **Actividades y espacios**, busca el nombre creado, selecciona su fila y pulsa **Inscribirme**, arriba del catálogo. Después de la confirmación se abre **Inscripciones**.
5. Dentro de **Inscripciones**, pulsa **Registrar participación**. Selecciona esa actividad, fecha de hoy, 100 DUNAB, evidencia `PRUEBA-TALLER-001` y semestre 6. Envía la solicitud.
6. Comprueba que el saldo aún no cambió y que la solicitud aparece pendiente.
7. Entra como administrador, selecciona la solicitud y apruébala con la observación `Participación ficticia verificada para la demostración`.
8. Vuelve al estudiante y consulta el semestre 6: debe aumentar 100 DUNAB. El histórico también aumenta 100. Los demás semestres conservan sus valores.
9. Cierra y abre la aplicación. Verifica que la inscripción, la decisión y el saldo continúan guardados.

Para repetir el ejemplo con la misma cuenta, crea otra actividad de prueba. No necesitas borrar ni reemplazar el archivo de almacenamiento.

## Si NetBeans sigue mostrando la versión anterior

Se verificó en las preferencias locales de **NetBeans 31** que el proyecto abierto apunta a esta misma carpeta corregida. En este equipo no necesitas copiar los archivos a otro proyecto: cierra la ejecución anterior, usa **Limpiar y construir** y después **Ejecutar proyecto (F6)**.

Si trabajas con una copia en otra carpeta, modificar esta carpeta de trabajo no actualiza automáticamente aquella copia. En ese caso:

1. Cierra la ejecución anterior.
2. Abre en NetBeans esta carpeta corregida o copia las fuentes y la configuración actualizadas a la carpeta de tu proyecto importado.
3. Confirma la clase principal `javaapplication1.MenuDUNAB` y la codificación UTF-8.
4. Ejecuta **Limpiar y construir** y después **Ejecutar proyecto (F6)**.

**Conserva `data`** en el directorio de trabajo que contiene tus pruebas. La actualización admite los registros guardados; no hace falta borrarlos para ampliar los semestres, cambiar el tema o utilizar la calculadora. El [menú original](<../../DUNAB_material_aparte/MenuDUNAB (1).java>) se conserva en la carpeta hermana `DUNAB_material_aparte`; la clase compilable y actualizada está en `src/javaapplication1/MenuDUNAB.java`.

Los originales, el PDF, el respaldo del menú y los scripts auxiliares se separaron sin borrar archivos. La carpeta aparte contiene [ARCHIVOS_MOVIDOS.json](../../DUNAB_material_aparte/ARCHIVOS_MOVIDOS.json) y [LEEME_MATERIAL.md](../../DUNAB_material_aparte/LEEME_MATERIAL.md). El proyecto conserva su código, configuración, datos y `tools/build.ps1`; `tmp/jdk` permanece porque ese script utiliza el JDK portable cuando lo necesita.

Las comprobaciones automáticas y las revisiones realmente ejecutadas se registran en [USO_IA.md](USO_IA.md). La apertura interactiva y las pruebas personales en NetBeans deben confirmarse desde el IDE del estudiante.
