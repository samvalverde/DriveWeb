document.addEventListener("DOMContentLoaded", () => {
  const quotaInput = document.getElementById("quota");

  // Permitir solo números positivos
  quotaInput.addEventListener("input", function () {
    this.value = this.value.replace(/[^0-9]/g, ''); // solo dígitos
    if (this.value < 1) this.value = 1;
  });
  const form = document.getElementById("formulario-registro");
  form.addEventListener("submit", registrar);
});




async function registrar(e) {
  e.preventDefault();

  const userInput = document.getElementById("user");
  const quotaInput = document.getElementById("quota");
  const mensaje = document.getElementById("mensaje");

  const user = document.getElementById("user").value.trim().toLowerCase();
  const quota = parseInt(quotaInput.value);

  // Validaciones
  if (!user) {
    mensaje.innerText = "Debes ingresar un nombre de usuario.";
    return;
  }

  if (isNaN(quota) || quota <= 0) {
    mensaje.innerText = "La cuota debe ser un número mayor a 0.";
    return;
  }

  // Verificar si ya existe ese usuario (normalize toLowerCase en backend también)
  const check = await fetch("api/command", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ action: "login", user })
  });

  if (check.status === 200) {
    mensaje.innerText = "Ese usuario ya existe. Intenta con otro nombre.";
    return;
  }

  // Crear el drive
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

    if (res.status === 409 || msg.includes("existe")) {
      // Usuario ya existe, no continuar
      return;
    }

    if (res.status === 200) {
      sessionStorage.setItem("usuario", user.toLowerCase());
      window.location.href = "drive.html";
    }


}