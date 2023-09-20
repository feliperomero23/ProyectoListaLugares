package com.example.proyectolistalugares

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberImagePainter
import com.example.proyectolistalugares.APIDOLAR.ApiService
import com.example.proyectolistalugares.APIDOLAR.TasaCambioResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime


enum class Pantalla{
    LISTALUGARES,
    INGRESARLUGAR,
    EDITARLUGAR,
    MOSTRARLUGAR
}

class ViewModelAPP: ViewModel() {
    val pantalla = mutableStateOf(Pantalla.LISTALUGARES) // Inicializa la pantalla actual como LISTALUGARES

    // Crea una instancia del servicio API
    private val apiService = ApiService.create()

    suspend fun obtenerTasaDeCambio(): TasaCambioResponse {
        return apiService.obtenerTasaCambio() // Realiza una llamada a la API para obtener la tasa de cambio

    }



    // Callbacks para el manejo de permisos
    var onPermisoCamaraOk: () -> Unit = {}
    var onPermisoUbicacionOk: () -> Unit = {}

    // lanzador permisos
    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null


    //Cambios de pantallas con viewmodels
    fun cambiarPantallaIngresolugar() {
        pantalla.value = Pantalla.INGRESARLUGAR
    }

    fun cambiarPantallalistadolugares() {
        pantalla.value = Pantalla.LISTALUGARES
    }
    fun mostrarlugarCompleto(){
        pantalla.value = Pantalla.MOSTRARLUGAR
    }
    fun cambiarEditarlugar(lugar: Lugar) {
        pantalla.value = Pantalla.EDITARLUGAR
    }
}

class MainActivity : ComponentActivity() {

    val VMAPP: ViewModelAPP by viewModels()
    lateinit var cameraController: LifecycleCameraController
    // Registro de un lanzador de permisos
    val lanzadorPermisos =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                // Verificación de permisos de ubicación
                (it[Manifest.permission.ACCESS_FINE_LOCATION] ?: false)
                        or (it[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso ubicacion granted")
                    VMAPP.onPermisoUbicacionOk()
                }


                else -> {
                }
            }
        }
    private lateinit var lugarDao: LugarDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            // Inicialización de lugarDao utilizando AppDatabase
            lugarDao = AppDatabase.getInstance(this@MainActivity).lugarDao()
            val lugaresbd = lugarDao.obtenertodosloslugares()
            withContext(Dispatchers.Main) {
                // Este bloque se ejecutará en el hilo principal
                VMAPP.cambiarPantallalistadolugares()
                VMAPP.lanzadorPermisos = lanzadorPermisos
                // Configuración de la interfaz de usuario utilizando AppUI
                setContent {
                    AppUI(
                        VMAPP = VMAPP,
                        lugarDao = lugarDao,


                    )
                }
            }
        }
    }
}



@Composable
fun AppUI(VMAPP: ViewModelAPP, lugarDao: LugarDao, onSave: () -> Unit = {}) {
    val contexto = LocalContext.current
    val (lugares) = remember { mutableStateOf(emptyList<Lugar>()) }
    val alcanceCorrutina = rememberCoroutineScope()

    when (VMAPP.pantalla.value) {
           Pantalla.LISTALUGARES-> {
               Listadodelugares(
                   VMAPP = VMAPP,
                   lugares = lugares,
                   onEditarClick = { lugar ->
                       VMAPP.cambiarEditarlugar(lugar)
                   },
                   onEliminarClick = { lugar ->
                       alcanceCorrutina.launch {
                          lugarDao.eliminarlugar(lugar)
                           VMAPP.cambiarPantallalistadolugares()
                       }
                   },
                   )
           }

        Pantalla.MOSTRARLUGAR -> {
            val lugarSeleccionado = Lugar(
                lugar = "Termas Geométricas",
                imagendeReferencia = "https://chiletermas.cl/valdivia/termas-geometricas-06.jpg",
                latitud = -39.5008911,
                longitud = -71.8742377,
                orden = 10,
                costoxAlojamiento = 55000.0,
                costoxTraslado = 15000.0,
                comentarios = "Cuenta con más de 20 piscinas de agua caliente con temperaturas que van desde los 35 a los 45 grados Celsius. Estas piscinas están construidas armoniosamente en el medio de un riachuelo llamado 'Cajón Negro'." // Comentarios o descripción del lugar
            )
            lugarSeleccionado?.let {
                InfoLugar(
                    VMAPP = VMAPP,
                    lugar = it,
                    onEditarClick = { lugar ->
                        VMAPP.cambiarEditarlugar(lugar)
                    },
                    onEliminarClick = { lugar ->
                        alcanceCorrutina.launch {
                            VMAPP.cambiarPantallalistadolugares()
                            onSave()
                        }
                    }
                )
            }
        }

        Pantalla.INGRESARLUGAR -> {
            PantallaIngresarnuevolugar(
                VMAPP = VMAPP,
                lugarDao = lugarDao,
                onGuardarClick = { newlugar ->
                    alcanceCorrutina.launch {
                        lugarDao.insertarlugar(newlugar)
                        VMAPP.cambiarPantallalistadolugares()
                    }
                }
            )
        }

        /*Pantalla.EDITARLUGAR -> {
            Pantallaeditarlugar(
                    VMAPP = VMAPP,
                    lugaredit = lugar,// Pasa el lugar que deseas editar
                    onSave = onSave, // Puedes proporcionar onSave si es necesario
                    lugarDao = lugarDao,
                    onActualizarclick = { editedLugar ->
                    alcanceCorrutina.launch {
                        lugarDao.actualizarlugar(editedLugar)
                        VMAPP.cambiarPantallalistadolugares()
                    }

                }
           )
        }
*/



        else -> {}
    }
}
// Configuaracion y visualizacion del listado de lugares ingresados a la base de datos.
@Composable
fun Listadodelugares(
    VMAPP: ViewModelAPP,
    onEditarClick: (Lugar) -> Unit,
    onEliminarClick: (Lugar) -> Unit,
    lugares: List<Lugar>,

){

    val contexto = LocalContext.current
    val (lugares, setLugares) = remember { mutableStateOf(emptyList<Lugar>()) }
    LaunchedEffect(lugares) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(contexto).lugarDao()
            setLugares(dao.obtenertodosloslugares())
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart) // Alinea la lista en la parte superior
        ) {
            items(lugares) { lugar ->
                PantallaListado(
                    lugar = lugar,
                    onEliminarClick = onEliminarClick,
                    onEditarClick = onEditarClick,
                    VMAPP = VMAPP

                ) {
                    setLugares(emptyList<Lugar>())
                }
            }
        }
        Button(
            onClick = {
                VMAPP.cambiarPantallaIngresolugar()
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Alinea el botón en la parte inferior
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar Lugar",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp)) // Espacio entre el icono y el texto
                Text(stringResource(R.string.agregar_lugar))
            }
        }
    }
}




// Configuaracion y visualizacion del listado de lugares ingresados a la base de datos.
@Composable
fun PantallaListado(VMAPP: ViewModelAPP,onEditarClick: (Lugar) -> Unit,onEliminarClick: (Lugar) -> Unit,lugar: Lugar,onSave: () -> Unit = {}){

    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .padding(0.dp)
            .clickable {
                VMAPP.mostrarlugarCompleto()
            }
            .background(Color.White)
            .padding(10.dp)

    ) {
        // Visualización de datos

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val painter = rememberImagePainter(
                data = lugar.imagendeReferencia,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error)
                }
            )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp) // Establecer el tamaño deseado
                    .offset(y = -5.dp) // Ajustar la posición vertical
                    .fillMaxSize() // Ajustar la imagen completamente dentro del tamaño especificado
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "${lugar.lugar}",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(bottom = 0.dp)
                        .offset(y = -5.dp)
                )
                Spacer(modifier = Modifier.height(0.dp))

                Text(
                    text = stringResource(R.string.costo_por_alojamiento, lugar.costoxAlojamiento),
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = stringResource(R.string.costo_por_traslado, lugar.costoxTraslado),
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Icono de geolocalización
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        modifier = Modifier
                            .clickable {

                            }
                    )

                    // Icono para editar
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier
                            .clickable {
                                VMAPP.cambiarEditarlugar(lugar)
                            }
                    )

                    // Botón para eliminar
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier

                            .clickable {
                                alcanceCorrutina.launch(Dispatchers.IO) {
                                    val dao = AppDatabase.getInstance(contexto).lugarDao()
                                    dao.eliminarlugar(lugar)
                                    onSave()
                                }
                            }
                    )
                }
            }
        }
    }
}
// Configuaracion y visualizacion del lugar seleccionado y mostrando mapa y detalles de lugar.
@Composable
fun InfoLugar(VMAPP:ViewModelAPP,onEditarClick: (Lugar) -> Unit,onEliminarClick: (Lugar) -> Unit,lugar: Lugar,


) {

    val contexto = LocalContext.current



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)

    ) {
        Text(
            text = lugar.lugar,
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(15.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberImagePainter(
                data = lugar.imagendeReferencia,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error)
                }
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp, 200.dp)

            )
        }


        Spacer(modifier = Modifier.height(15.dp))

        androidx.compose.material.Text(
            text = stringResource(R.string.costo_por_alojamiento, lugar.costoxAlojamiento),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ), // Ajustamos el tamaño de la fuente

            modifier = Modifier.padding(bottom = 8.dp) // Espacio en la parte inferior
        )
        androidx.compose.material.Text(
            text = stringResource(R.string.costo_por_traslado, lugar.costoxTraslado),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ), // Ajustamos el tamaño de la fuente
            modifier = Modifier.padding(bottom = 8.dp) // Espacio en la parte inferior
        )
        androidx.compose.material.Text(
            text = "${lugar.comentarios}",
            style = TextStyle(
                fontSize = 15.sp

            ), // Ajustamos el tamaño de la fuente
            modifier = Modifier.padding(bottom = 16.dp) // Espacio en la parte inferior
        )

        // Iconos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Espacio uniforme en todos los lados
            horizontalArrangement = Arrangement.Center
        ) {
            // Icono para tomar foto (editar según sea necesario)
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = "Tomar Foto",
                modifier = Modifier
                    .clickable {

                    }
                    .size(30.dp) // Tamaño del icono
            )
            Spacer(modifier = Modifier.width(32.dp)) // Espacio entre los iconos
            // Icono para editar (editar según sea necesario)
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                modifier = Modifier
                    .clickable {
                        onEditarClick(lugar)
                    }
                    .size(30.dp) // Tamaño del icono
            )
            Spacer(modifier = Modifier.width(32.dp)) // Espacio entre los iconos
            // Icono para eliminar (editar según sea necesario)
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar Producto",
                modifier = Modifier
                    .clickable {
                        onEliminarClick(lugar)
                    }
                    .size(30.dp) // Tamaño del icono
            )
        }

        // Agrega espacio entre los iconos y el mapa
        Spacer(modifier = Modifier.height(40.dp))


        // Mapa (segun latitud y longuitud ingresada)
        AndroidView(
            factory = {
                MapView(it).also { mapView ->
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    Configuration.getInstance().userAgentValue =
                        contexto.packageName

                    mapView.controller.setZoom(18.0)
                    val geoPoint = GeoPoint(lugar.latitud, lugar.longitud)
                    mapView.controller.animateTo(geoPoint)

                    val marcador = Marker(mapView)
                    marcador.position = geoPoint
                    marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    mapView.overlays.add(marcador)

                    mapView.setClickable(false)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                //.width(230.dp)
                .height(90.dp)

                //.offset(y = -35.dp, x = 71.dp)


        )

    }
    Button(
        onClick = {
            VMAPP.cambiarPantallalistadolugares()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .offset(y = 690.dp, x = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.KeyboardReturn,
                contentDescription = "Agregar Lugar",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp)) // Espacio entre el icono y el texto
            Text(stringResource(R.string.volver))
        }
    }
}
// Configuaracion y visualizacion de la pantalla para ingresar un nuevo lugar turistico a la base de datos a la base de datos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaIngresarnuevolugar(VMAPP: ViewModelAPP,
                               onGuardarClick: (Lugar) -> Unit,
                               onSave: () -> Unit = {},
                               lugarDao: LugarDao) {

    var lugar by remember { mutableStateOf("") }
    var imagenReferencia by remember { mutableStateOf("") }
    var latitud by remember { mutableStateOf(0.0) }
    var longitud by remember { mutableStateOf(0.0) }
    var orden by remember { mutableStateOf(0) }
    var costoAlojamiento by remember { mutableStateOf(0.0) }
    var costoTraslado by remember { mutableStateOf(0.0) }
    var comentario by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = lugar,
            onValueChange = { lugar = it },
            label = { Text(stringResource(R.string.lugar)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = imagenReferencia,
            onValueChange = { imagenReferencia = it },
            label = { Text(stringResource(R.string.imagen_de_referencia_url)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = latitud.toString(),
            onValueChange = { latitud = it.toDoubleOrNull() ?: 0.0 },
            label = { Text(stringResource(R.string.latitud)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = longitud.toString(),
            onValueChange = { longitud = it.toDoubleOrNull() ?: 0.0 },
            label = { Text(stringResource(R.string.longitud)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = orden.toString(),
            onValueChange = { orden = it.toIntOrNull() ?: 0 },
            label = { Text(stringResource(R.string.orden)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = costoAlojamiento.toString(),
            onValueChange = { costoAlojamiento = it.toDoubleOrNull() ?: 0.0 },
            label = { Text(stringResource(R.string.costo_alojamiento)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = costoTraslado.toString(),
            onValueChange = { costoTraslado = it.toDoubleOrNull() ?: 0.0 },
            label = { Text(stringResource(R.string.costo_traslado)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = comentario,
            onValueChange = { comentario = it },
            label = { Text(stringResource(R.string.comentario)) },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val newlugar = Lugar(
                            lugar = lugar,
                            imagendeReferencia = imagenReferencia,
                            latitud = latitud,
                            longitud = longitud,
                            orden = orden,
                            costoxAlojamiento = costoAlojamiento,
                            costoxTraslado = costoTraslado,
                            comentarios = comentario
                        )
                        onGuardarClick(newlugar)
                        onSave()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Guardar",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.guardar))
                }
                Spacer(modifier = Modifier.height(5.dp))
                Button(
                    onClick = {
                        VMAPP.cambiarPantallalistadolugares()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardReturn,
                            contentDescription = "Volver",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.volver))
                    }
                }
            }
        }
    }
}

// Configuaracion y visualizacion para editar un lugar en la base de datos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantallaeditarlugar (
    VMAPP: ViewModelAPP,
    lugaredit: Lugar,
    onSave: () -> Unit = {},
    lugarDao: LugarDao,
    onActualizarclick:(Lugar)-> Unit
) {
    val contexto = LocalContext.current
    var editedLugar by remember { mutableStateOf(lugaredit.lugar) }
    var editedImagenReferencia by remember { mutableStateOf(lugaredit.imagendeReferencia) }
    var editedLatitud by remember { mutableStateOf(lugaredit.latitud.toString()) }
    var editedLongitud by remember { mutableStateOf(lugaredit.longitud.toString()) }
    var editedOrden by remember { mutableStateOf(lugaredit.orden.toString()) }
    var editedCostoAlojamiento by remember { mutableStateOf(lugaredit.costoxAlojamiento.toString()) }
    var editedCostoTraslado by remember { mutableStateOf(lugaredit.costoxTraslado.toString()) }
    var editedComentario by remember { mutableStateOf(lugaredit.comentarios) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Campos de edición
        Spacer(modifier = Modifier.width(20.dp))

        TextField(
            value = editedLugar,
            onValueChange = { editedLugar = it },
            label = { Text("Lugar") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = editedImagenReferencia,
            onValueChange = { editedImagenReferencia = it },
            label = { Text("Imagen de Referencia") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = editedLatitud,
            onValueChange = { editedLatitud = it },
            label = { Text("Latitud") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = editedLongitud,
            onValueChange = { editedLongitud = it },
            label = { Text("Longitud") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = editedOrden,
            onValueChange = { editedOrden = it },
            label = { Text("Orden") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = editedCostoAlojamiento,
            onValueChange = { editedCostoAlojamiento = it },
            label = { Text("Costo Alojamiento") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = editedCostoTraslado,
            onValueChange = { editedCostoTraslado = it },
            label = { Text("Costo Traslado") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = editedComentario,
            onValueChange = { editedComentario = it },
            label = { Text("Comentario") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(60.dp))
        // Botón para actualizar el registro
        Button(
            onClick = {
                // Guardar los cambios y actualizar el registro

                val editedLugar= Lugar (
                    lugar = editedLugar,
                    imagendeReferencia = editedImagenReferencia,
                    latitud = editedLatitud.toDoubleOrNull() ?: 0.0,
                    longitud = editedLongitud.toDoubleOrNull() ?: 0.0,
                    orden = editedOrden.toIntOrNull() ?: 0,
                    costoxAlojamiento = editedCostoAlojamiento.toDoubleOrNull() ?: 0.0,
                    costoxTraslado = editedCostoTraslado.toDoubleOrNull() ?: 0.0,
                    comentarios = editedComentario
                )
                onActualizarclick(editedLugar)
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)

        ) {
            Text("Actualizar Registro")

        }
        Spacer(modifier = Modifier.height(5.dp))

        Button(
            onClick = {
                // Cambiar la pantalla al formulario principal
                VMAPP.cambiarPantallalistadolugares()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text("Volver")
        }
    }
}




















