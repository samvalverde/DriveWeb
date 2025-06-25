const usuario = sessionStorage.getItem("usuario");
if (!usuario) window.location.href = "login.html";
document.getElementById("user").innerText = usuario;

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
    const name = document.getElementById("name").value.trim();
    const content = document.getElementById("content").value;
    const errorBox = document.getElementById("errorArchivo");

    if (!name) {
        errorBox.innerText = "⚠️ El nombre del archivo es obligatorio.";
        return;
    }

    const respuesta = await enviarComando({ action: "createFile", user: usuario, name, content });

    // Verifica la respuesta del servidor
    if (respuesta.includes("Ya existe")) {
        errorBox.innerText = "Ya existe un archivo o directorio con ese nombre.";
    } else if (respuesta.includes("inválido")) {
        errorBox.innerText = "Nombre de archivo invalido. Usa una extension, por ejemplo 'nota.txt'.";
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
    const name = document.getElementById("name").value;
    await enviarComando({ action: "createDir", user: usuario, name });
    cerrarModal(); listar();
}

async function copiar(source, target) {
    await enviarComando({ action: "copy", user: usuario, source, target });
    cerrarModal(); listar();
}

async function mover(source, target) {
    await enviarComando({ action: "move", user: usuario, source, target });
    cerrarModal(); listar();
}

async function subirArchivo() {
    const fileInput = document.getElementById("fileInput");
    const file = fileInput.files[0];
    if (!file) return alert("Selecciona un archivo");

    const formData = new FormData();
    formData.append("action", "load");
    formData.append("user", usuario);
    formData.append("file", file);

    const res = await fetch("api/command", {
        method: "POST",
        body: formData
    });

    const resultado = await res.text();
    alert(resultado);
    cerrarModal();
    listar();
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
    
    if (ruta.trim().startsWith("Comando no válido")) {
        document.getElementById("ruta").innerText = "/";
    } else {
        document.getElementById("ruta").innerText = ruta;
    }

    const contenedor = document.getElementById("driveArea");
    contenedor.innerHTML = "";

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
        if (tipo === "folder") {
            div.onclick = () => entrar(nombre);
        } else {
            div.onclick = async () => {
                const contenido = await enviarComando({ action: "viewFile", user: usuario, name: nombre });
                mostrarVistaArchivo(nombre, contenido);
            };

        }
        contenedor.appendChild(div);
    }

    if (ruta.trim() === "/") {
        const shared = document.createElement("div");
        shared.className = "item";
        shared.innerHTML = `<i class="fa-solid fa-folder-open"></i><div class="name">shared</div>`;
        shared.onclick = () => entrar("shared");
        contenedor.appendChild(shared);
    }
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

function mostrarVistaArchivo(nombre, contenido) {
  const m = document.createElement("div");
  m.className = "modal";

  m.innerHTML = `
    <div class="modal-content">
      <h3>${nombre}</h3>
      <textarea readonly style="width: 100%; height: 200px;">${contenido}</textarea>
      <button onclick="cerrarModal()">Cerrar</button>
    </div>
  `;

  m.onclick = e => { if (e.target === m) m.remove(); };
  document.body.appendChild(m);
}


listar();
