window.addEventListener("load", event => {
    document
        .getElementById("create_space")
        .addEventListener("submit", createSpace);
})

function createSpace(event) {
    event.preventDefault();
    const space = document.getElementById("space").value;
    const owner = document.getElementById("owner").value;
    const token = localStorage.getItem('token')

    fetch('https://localhost:4567/spaces', {
        method: 'POST',
        body: JSON.stringify({name: space, owner: owner}),
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    }).then(response => {
        if (response.ok) {
            document.getElementById("space").value = '';
            document.getElementById("owner").value = '';
            return response.json();
        } else {
            throw new Error(response.statusText)
        }
    }).then(json => {
        let success = document.getElementById("s_response");
        success.style.display = 'unset';
        success.style.color = 'green';
        success.innerText = `Space ${json.name} successfully created. Available at ${json.uri}`
    }).catch(error => {
        let success = document.getElementById("s_response");
        success.style.display = 'unset';
        success.style.color = 'red';
        success.innerText = `Errore creating space.`
        console.error(`Error: ${error}`)
    });
}