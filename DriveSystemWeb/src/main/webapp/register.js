async function registrar(e) {
  e.preventDefault();
  const user = document.getElementById("user").value;
  const quota = document.getElementById("quota").value;

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
  document.getElementById("mensaje").innerText = msg;

  if (res.status === 200) {
    sessionStorage.setItem("usuario", user);
    window.location.href = "drive.html";
  }
}
