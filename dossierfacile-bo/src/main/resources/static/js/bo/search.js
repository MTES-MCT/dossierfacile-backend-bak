 document.addEventListener('DOMContentLoaded', function () {
            const keySearch = document.getElementById('search-text').innerText;
            document.querySelectorAll('.to-highlight')
            const elementsToHighlight = document.querySelectorAll('.to-highlight');
            const regex = new RegExp(`${keySearch}`, 'gi');
            elementsToHighlight.forEach(element => {
                element.innerHTML = element.innerHTML.replace(regex, "<span class='highlight'>$&</span>");
            });
        });