document.getElementById("myForm").addEventListener("submit", function(event) {
    event.preventDefault();

    let name = document.getElementById("name").value;
    let age = document.getElementById("age").value;
    let animal = document.querySelector('input[name="animal"]:checked').value;

    fetch(`/hello?name=${name}&age=${age}&animal=${animal}`)
        .then(response => response.text())
        .then(data => {
            let table = document.getElementById("resultTable").getElementsByTagName('tbody')[0];
            let newRow = table.insertRow();

            let cell1 = newRow.insertCell(0);
            let cell2 = newRow.insertCell(1);
            let cell3 = newRow.insertCell(2);

            cell1.textContent = name;
            cell2.textContent = age;
            cell3.textContent = animal.charAt(0).toUpperCase() + animal.slice(1);
        })
        .catch(error => console.error("Error:", error));

    // Limpiar el formulario después de enviar los datos
    document.getElementById("myForm").reset();
});
