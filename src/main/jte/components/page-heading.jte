@import gg.jte.Content

@param String section
@param String title
@param String description
@param Content actions = null

<div class="d-flex align-items-start justify-content-between flex-wrap gap-3 mb-4">
    <div>
        <p class="small fw-semibold text-body-secondary mb-2">${section}</p>
        <h1 class="h2 fw-bold mb-2">${title}</h1>
        <p class="text-body-secondary mb-0">${description}</p>
    </div>
    @if(actions != null)
        <div class="d-flex gap-2">${actions}</div>
    @endif
</div>
