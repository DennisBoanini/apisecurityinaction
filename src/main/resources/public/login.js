window.addEventListener("load", event => {
    document
        .getElementById("login")
        .addEventListener("submit", login);
})

function login(event) {
    event.preventDefault();
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const credentials = `Basic ${btoa(username + ':' + password)}`;

    fetch('https://localhost:4567/sessions', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': credentials
        }
    }).then(response => {
        if (response.ok) {
            response.json()
                .then(json => localStorage.setItem('token', json.token))
            window.location.replace('/natter.html');
        }
    }).catch(error => console.error(`Error: ${error}`));
}