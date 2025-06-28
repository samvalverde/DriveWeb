const usuario = sessionStorage.getItem("usuario").toLowerCase();
if (!usuario) window.location.href = "login.html";
document.getElementById("user").innerText = usuario;
let rutaActual = "/";

function logout() {
    sessionStorage.clear();
    window.location.href = 'login.html';
}

function mostrarModal(tipo) {
    const m = document.createElement("div");
    m.className = "modal";

    let contenido = "";
    if (tipo === 'archivo') {
    contenido = `
        <div class='modal-content'>
            <h3>Nuevo Archivo</h3>
            <input id='name' placeholder='Nombre.txt'>
            <textarea id='content' placeholder='Contenido'></textarea>
            <div id="errorArchivo" style="color: red; font-size: 0.9rem; margin-top: 0.5rem;"></div>
            <button onclick='crearArchivo()'>Crear</button>
        </div>`;
    } else if (tipo === 'carpeta') {
        contenido = `
        <div class='modal-content'>
            <h3>Nueva Carpeta</h3>
            <input id='name' placeholder='Nombre'>
            <button onclick='crearDirectorio()'>Crear</button>
        </div>`;
    } else if (tipo === 'copiar' || tipo === 'mover') {
        contenido = `
        <div class='modal-content'>
            <h3>${tipo === 'copiar' ? 'Copiar' : 'Mover'}</h3>
            <input id='source' placeholder='Nombre de origen'>
            <input id='target' placeholder='Directorio destino'>
            <button onclick='${tipo}(document.getElementById("source").value, document.getElementById("target").value)'>Aceptar</button>
        </div>`;
    } else if (tipo === 'subir') {
        contenido = `
        <div class='modal-content'>
            <h3>Subir Archivo</h3>
            <input type='file' id='fileInput'>
            <button onclick='subirArchivo()'>Subir</button>
        </div>`;
    } else if (tipo === 'eliminar') {
        contenido = `
        <div class='modal-content'>
            <h3>Eliminar</h3>
            <input id='name' placeholder='Nombre a eliminar'>
            <button onclick='eliminar()'>Eliminar</button>
        </div>`;
    } else if (tipo === 'compartir') {
        contenido = `
        <div class='modal-content'>
            <h3>Compartir</h3>
            <input id='name' placeholder='Archivo o carpeta a compartir'>
            <input id='toUser' placeholder='Usuario destino'>
            <button onclick='compartir()'>Compartir</button>
        </div>`;
    }

    m.innerHTML = contenido;
    m.onclick = e => { if (e.target === m) m.remove(); };
    document.body.appendChild(m);
}

function cerrarModal() {
    const m = document.querySelector(".modal");
    if (m) m.remove();
}

async function crearArchivo() {
    const name = document.getElementById("name").value.trim().toLowerCase();
    const content = document.getElementById("content").value;
    const errorBox = document.getElementById("errorArchivo");

    if (!name) {
        errorBox.innerText = "⚠️ El nombre del archivo es obligatorio.";
        return;
    }

    const respuesta = await enviarComando({ action: "createFile", user: usuario, name, content });

    if (respuesta.includes("Ya existe")) {
        const confirmar = confirm("⚠️ Ya existe un archivo o carpeta con ese nombre. ¿Desea sobrescribirlo?");
        if (confirmar) {
            // Si el usuario acepta, eliminamos el anterior y creamos uno nuevo
            await enviarComando({ action: "delete", user: usuario, name });
            await enviarComando({ action: "createFile", user: usuario, name, content });
            cerrarModal();
            listar();
        } else {
            errorBox.innerText = "No se creó el archivo.";
        }
    } else if (respuesta.includes("inválido")) {
        errorBox.innerText = "Nombre de archivo inválido. Usa una extensión, por ejemplo 'nota.txt'.";
    } else if (respuesta.includes("espacio")) {
        errorBox.innerText = "No hay espacio suficiente para crear el archivo.";
    } else if (respuesta.includes("Archivo creado")) {
        cerrarModal();
        listar();
    } else {
        errorBox.innerText = "Error inesperado: " + respuesta;
    }
}

async function crearDirectorio() {
    const name = document.getElementById("name").value.trim().toLowerCase();
    if (!name) return;

    const respuesta = await enviarComando({ action: "createDir", user: usuario, name });

    if (respuesta.includes("Ya existe")) {
        const confirmar = confirm("⚠️ Ya existe un archivo o carpeta con ese nombre. ¿Desea sobrescribirlo?");
        if (confirmar) {
            await enviarComando({ action: "delete", user: usuario, name });
            await enviarComando({ action: "createDir", user: usuario, name });
            cerrarModal();
            listar();
        }
    } else {
        cerrarModal();
        listar();
    }
}

async function copiar(source, target) {
    await enviarComando({ action: "copy", user: usuario, source, target });
    cerrarModal(); listar();
}

async function mover(source, target) {
    await enviarComando({ action: "move", user: usuario, source, target });
    cerrarModal(); listar();
}

function descargarArchivo(nombre, contenido) {
  const blob = new Blob([contenido], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  URL.revokeObjectURL(url);
}

async function subirArchivo() {
    const fileInput = document.getElementById("fileInput");
    const file = fileInput.files[0];
    if (!file) {
        alert("Selecciona un archivo");
        return;
    }

    // Validar extensión .txt
    if (!file.name.endsWith(".txt")) {
        alert("Solo se permiten archivos de texto (.txt)");
        return;
    }

    const formData = new FormData();
    formData.append("action", "load");
    formData.append("user", usuario);
    formData.append("file", file);

    try {
        const res = await fetch("api/command", {
            method: "POST",
            body: formData
        });

        const resultado = await res.text();
        alert(resultado);
        cerrarModal();
        listar();
    } catch (err) {
        alert("Error al subir el archivo: " + err.message);
    }
}

async function mostrarUsoEspacio() {
    const resultado = await enviarComando({ action: "usage", user: usuario });
    document.getElementById("espacio").innerText = resultado;
}


async function eliminar() {
    const name = document.getElementById("name").value;
    await enviarComando({ action: "delete", user: usuario, name });
    cerrarModal(); listar();
}

async function compartir() {
    const name = document.getElementById("name").value;
    const target = document.getElementById("toUser").value;
    await enviarComando({ action: "share", user: usuario, name, target });
    cerrarModal(); listar();
}

async function subir() {
    await enviarComando({ action: "cd", user: usuario, name: ".." });
    listar();
}

async function entrar(nombre) {
    await enviarComando({ action: "cd", user: usuario, name: nombre });
    listar();
}

async function listar() {
    const res = await enviarComando({ action: "listDir", user: usuario });
    const ruta = await enviarComando({ action: "pwd", user: usuario });

    rutaActual = ruta.trim().replace(/^\/+/, ""); // sin '/' inicial
    document.getElementById("ruta").innerText = ruta || "/";

    const contenedor = document.getElementById("driveArea");
    contenedor.innerHTML = "";

    // Botón para subir
    const back = document.createElement("div");
    back.className = "item";
    back.innerHTML = `<i class="fa-solid fa-level-up-alt"></i><div class="name">..</div>`;
    back.onclick = subir;
    contenedor.appendChild(back);

    const lineas = res.split("\n").filter(l => l.trim() !== "");
    for (let l of lineas) {
        const tipo = l.startsWith("[DIR]") ? "folder" : "file";
        const nombre = l.replace("[DIR] ", "").replace("[FILE] ", "");

        const div = document.createElement("div");
        div.className = "item";
        div.innerHTML = `<i class="fa-solid fa-${tipo === 'folder' ? 'folder' : 'file'}"></i><div class="name">${nombre}</div>`;

        // Click izquierdo para abrir
        if (tipo === "folder") {
            div.onclick = () => entrar(nombre);
        } else {
            div.onclick = async () => {
                const contenido = await enviarComando({ action: "viewFile", user: usuario, name: nombre });
                mostrarVistaArchivo(nombre, contenido);
            };
        }

        // Menú contextual con click derecho
        div.oncontextmenu = (e) => {
            e.preventDefault();
            mostrarMenuContextual(nombre, tipo);
        };

        contenedor.appendChild(div);
    }
    
    // Mostrar el uso del espacio
    await mostrarUsoEspacio();
}

async function enviarComando(params) {
    const form = new URLSearchParams();
    for (let key in params) form.append(key, params[key]);
    const res = await fetch("api/command", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: form
    });
    return res.text();
}

function mostrarMenuContextual(nombre, tipo) {
    cerrarMenuContextual();

    const menu = document.createElement("div");
    menu.className = "context-menu";
    menu.style.top = `${event.clientY}px`;
    menu.style.left = `${event.clientX}px`;

    menu.innerHTML = `
        <div onclick="mostrarFormularioPropiedades('${nombre}')">🔍 Ver propiedades</div>
        ${tipo === 'file' ? `
        <div onclick="mostrarFormularioEditar('${nombre}')">✏️ Modificar</div>
        <div onclick="descargarArchivoDesdeServidor('${nombre}')">📥 Descargar</div>
    ` : ''}
        <div onclick="mostrarFormularioCopiar('${nombre}')">📋 Copiar</div>
        <div onclick="mostrarFormularioMover('${nombre}')">📂 Mover</div>
        <div onclick="mostrarFormularioEliminar('${nombre}')">🗑️ Eliminar</div>
        <div onclick="mostrarFormularioCompartir('${nombre}')">🤝 Compartir</div>
    `;

    document.body.appendChild(menu);

    document.onclick = cerrarMenuContextual;
}

async function mostrarFormularioEditar(nombre) {
    cerrarMenuContextual();

    const contenido = await enviarComando({ action: "viewFile", user: usuario, name: nombre });

    const m = document.createElement("div");
    m.className = "modal";

    m.innerHTML = `
        <div class="modal-content">
            <h3>Editar: ${nombre}</h3>
            <textarea id="editarContenido" style="width: 100%; height: 200px;">${contenido}</textarea>
            <button onclick="guardarEdicion('${nombre}')">Guardar</button>
            <button onclick="cerrarModal()">Cancelar</button>
        </div>
    `;

    m.onclick = e => { if (e.target === m) m.remove(); };
    document.body.appendChild(m);
}

async function guardarEdicion(nombre) {
    const nuevoContenido = document.getElementById("editarContenido").value;
    const respuesta = await enviarComando({
        action: "editFile",
        user: usuario,
        name: nombre,
        content: nuevoContenido
    });

    alert(respuesta);
    cerrarModal();
    listar();
}

function cerrarMenuContextual() {
    const existing = document.querySelector(".context-menu");
    if (existing) existing.remove();
}

function mostrarFormularioCopiar(nombre) {
    cerrarMenuContextual();
    const destino = prompt(`¿A qué directorio quieres copiar '${nombre}'? Ej: prueba/docs`);
    if (destino) {
        const sourcePath = rutaActual ? `${rutaActual}/${nombre}` : nombre;
        copiarRuta(sourcePath, destino);
    }
}

function mostrarFormularioMover(nombre) {
    cerrarMenuContextual();
    const destino = prompt(`¿A qué directorio quieres mover '${nombre}'?`);
    if (destino) {
        const sourcePath = rutaActual ? `${rutaActual}/${nombre}` : nombre;
        moverRuta(sourcePath, destino);
    }
}

async function copiarRuta(source, target) {
    const res = await enviarComando({ action: "copy", user: usuario, source, target });
    alert(res);
    listar();
}

async function moverRuta(source, target) {
    const res = await enviarComando({ action: "move", user: usuario, source, target });
    alert(res);
    listar();
}

function mostrarFormularioEliminar(nombre) {
    cerrarMenuContextual();
    if (confirm(`¿Eliminar '${nombre}'?`)) eliminarDirecto(nombre);
}

async function eliminarDirecto(name) {
    await enviarComando({ action: "delete", user: usuario, name });
    listar();
}

function mostrarFormularioCompartir(nombre) {
    cerrarMenuContextual();
    const target = prompt(`¿A qué usuario deseas compartir '${nombre}'?`);
    if (target) {
        enviarComando({ action: "share", user: usuario, name: nombre, target })
            .then(alert)
            .then(listar);
    }
}

async function descargarArchivoDesdeServidor(nombre) {
    cerrarMenuContextual();
    const contenido = await enviarComando({ action: "viewFile", user: usuario, name: nombre });
    descargarArchivo(nombre, contenido);
}

function mostrarVistaArchivo(nombre, contenido) {
  const m = document.createElement("div");
  m.className = "modal";

  m.innerHTML = `
    <div class="modal-content">
      <h3>${nombre}</h3>
      <textarea readonly style="width: 100%; height: 200px;">${contenido}</textarea>
      <div style="margin-top: 10px;">
        <button onclick="cerrarModal()">Cerrar</button>
      </div>
    </div>
  `;

  m.onclick = e => { if (e.target === m) m.remove(); };
  document.body.appendChild(m);
}

async function mostrarFormularioPropiedades(nombre) {
    cerrarMenuContextual();

    const resultado = await enviarComando({ action: "properties", user: usuario, name: nombre });

    const m = document.createElement("div");
    m.className = "modal";

    m.innerHTML = `
        <div class="modal-content">
            <h3>Propiedades de: ${nombre}</h3>
            <pre>${resultado}</pre>
            <button onclick="cerrarModal()">Cerrar</button>
        </div>
    `;

    m.onclick = e => { if (e.target === m) m.remove(); };
    document.body.appendChild(m);
}



listar();
