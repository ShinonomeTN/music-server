<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<link rel="icon" href="/favicon.ico">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet"
    integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3" crossorigin="anonymous">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css" />
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.8.1/font/bootstrap-icons.css">
<style>
    body {
        /* background: darkorange; */
        background: var(--ms-base-color, #FFF);
    }

    .msc-card {
        /* background: var(--bs-dark); */
        background: #FFF;
        color: #000;
        border: 1px solid #999;
        border-radius: 5px;
        padding: 10pt;
        transition: all 0.3s ease;
        overflow: hidden;
        /* box-shadow: 0 0 10pt white; */
    }

    .msc-main {
        margin-top: 40pt;
        margin-bottom: 40pt;
        min-width: 200pt;
        min-height: 300pt;
    }

    @media (min-width: 768px) {
        .msc-main {
            max-width: 400pt;
        }
    }

    .msc-flex-row-center {
        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: center;
    }

    .msc-flex-column {
        display: flex;
        flex-direction: column;
    }

    .msc-flex-column.center {
        align-items: center;
    }

    .ani-fade-up-enter-active {
        position: relative;
        animation: fadeInRight 0.3s;
    }

    .ani-fade-up-leave-active {
        position: relative;
        animation: fadeOutLeft 0.3s
    }

    .data-late-show {
        visibility: hidden;
    }
    .data-late-show.data-late-shown {
        visibility: visible !important;
    }
</style>
