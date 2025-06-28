async function login(e) {
  e.preventDefault();
  const user = document.getElementById("usuario").value;
  const form = new URLSearchParams();
  form.append("action", "login");
  form.append("user", user);

  const res = await fetch("api/command", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form
  });

  if (res.status === 200) {
    sessionStorage.setItem("usuario", user);
    window.location.href = "drive.html";
  } else {
    document.getElementById("error").innerText =
      "Usuario no registrado. Debe registrarse primero.";
  }
}