@import gg.jte.Content
@import dev.template.application.web.ui.UiPage

@param UiPage page
@param String title
@param Content content

<!DOCTYPE html>
<html lang="ja" data-bs-theme="light" x-data="workspaceShell" x-bind:data-bs-theme="dark ? 'dark' : 'light'">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="csrf-header" content="${page.csrfHeader()}">
        <meta name="htmx-config" content='{"historyCacheSize":0,"allowEval":false,"includeIndicatorStyles":false}'>
        <title>${title} | Spring Application Template</title>
        <script src="${page.contextPath()}/ui/application.js"></script>
        <link rel="stylesheet" href="${page.contextPath()}/vendor/bootstrap.min.css">
        <link rel="stylesheet" href="${page.contextPath()}/ui/application.css">
        <script defer src="${page.contextPath()}/vendor/bootstrap.min.js"></script>
        <script defer src="${page.contextPath()}/vendor/htmx.min.js"></script>
        <script defer src="${page.contextPath()}/vendor/cdn.min.js"></script>
    </head>
    <body hx-history="false" class="bg-body-tertiary">
        <a class="visually-hidden-focusable" href="#main">本文へ移動</a>
        ${content}
    </body>
</html>
