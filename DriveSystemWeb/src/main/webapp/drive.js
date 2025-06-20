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
    const name = document.getElementById("name").value;
    const content = document.getElementById("content").value;
    await enviarComando({ action: "createFile", user: usuario, name, content });
    cerrarModal(); listar();
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
    document.getElementById("ruta").innerText = ruta;

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
                alert("Contenido de archivo:\n\n" + contenido);
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

listar();
