(function () {
  const dialog = document.getElementById("access-dialog");
  const form = document.getElementById("access-form");
  const status = document.getElementById("access-form-status");
  const approvalNote = document.getElementById("sandbox-approval-note");
  const openButtons = document.querySelectorAll("[data-open-access-dialog]");
  const closeButtons = document.querySelectorAll("[data-close-access-dialog]");
  let approvalNoteTimeout = null;

  if (!dialog || !form || !status) {
    return;
  }

  openButtons.forEach((button) => {
    button.addEventListener("click", () => {
      status.textContent = "";
      status.dataset.state = "";
      hideApprovalNote();
      dialog.showModal();
    });
  });

  closeButtons.forEach((button) => {
    button.addEventListener("click", () => {
      dialog.close();
    });
  });

  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
      dialog.close();
    }
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitButton = form.querySelector('button[type="submit"]');
    const payload = Object.fromEntries(new FormData(form).entries());
    payload.requestedResources = Array.from(form.querySelectorAll('input[name="requestedResourceOption"]:checked'))
      .map((input) => input.value)
      .join(", ");
    delete payload.requestedResourceOption;

    status.dataset.state = "";
    status.textContent = "Enviando solicitação...";
    submitButton.disabled = true;

    try {
      const response = await fetch("/api/developer-access-requests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify(payload),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || "Não foi possível registrar a solicitação agora.");
      }
      status.dataset.state = "success";
      status.textContent = data.message || "Solicitação registrada. Entraremos em contato pelo email informado.";
      showApprovalNote();
      form.reset();
    } catch (error) {
      status.dataset.state = "error";
      status.textContent = error.message || "Erro ao registrar solicitação. Tente novamente em instantes.";
      hideApprovalNote();
    } finally {
      submitButton.disabled = false;
    }
  });

  function showApprovalNote() {
    if (!approvalNote) {
      return;
    }
    approvalNote.hidden = false;
    window.clearTimeout(approvalNoteTimeout);
    approvalNoteTimeout = window.setTimeout(() => {
      approvalNote.hidden = true;
    }, 14000);
  }

  function hideApprovalNote() {
    if (!approvalNote) {
      return;
    }
    window.clearTimeout(approvalNoteTimeout);
    approvalNote.hidden = true;
  }
})();
