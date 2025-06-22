  document.addEventListener("DOMContentLoaded", () => {
  const quotaInput = document.getElementById("quota");

  // Bloquea entrada por teclado (excepto flechas ↑ ↓)
  quotaInput.addEventListener("keydown", function(e) {
    // Permitir flechas ↑ ↓, tab, backspace
    const allowedKeys = ["ArrowUp", "ArrowDown", "Tab", "Backspace"];
    if (!allowedKeys.includes(e.key)) {
      e.preventDefault();
    }
  });

  // Corrige si se pega número negativo o cero
  quotaInput.addEventListener("input", function () {
    if (this.value < 1) this.value = 1;
  });
});


async function registrar(e) {
  e.preventDefault();
  const user = document.getElementById("user").value.trim();
  const quota = parseInt(document.getElementById("quota").value);
  const mensaje = document.getElementById("mensaje");

  // Validaciones
  if (!user) {
    mensaje.innerText = "⚠️ Debes ingresar un nombre de usuario.";
    return;
  }

  if (isNaN(quota) || quota <= 0) {
    mensaje.innerText = "⚠️ La cuota debe ser un número mayor a 0.";
    return;
  }

  const form = new URLSearchParams();
  form.append("action", "createDrive");
  form.append("user", user);
  form.append("quota", quota);

  const res = await fetch("api/command", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form
  });

  const msg = await res.text();
  mensaje.innerText = msg;

  if (res.status === 200) {
    sessionStorage.setItem("usuario", user);
    window.location.href = "drive.html";
  }
}
