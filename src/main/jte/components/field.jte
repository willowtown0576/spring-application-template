@param String id
@param String label
@param String value = ""
@param String error = ""
@param String type = "text"
@param boolean required = false

<div class="mb-3">
    <label class="form-label" for="${id}">
        ${label}
        @if(required)
            <span class="text-danger">必須</span>
        @endif
    </label>
    <input
        class="form-control ${error.isEmpty() ? "" : "is-invalid"}"
        id="${id}"
        name="${id}"
        type="${type}"
        value="${value}"
        required="${required}"
        aria-invalid="${!error.isEmpty()}"
        aria-describedby="${id}-error">
    <div id="${id}-error" class="invalid-feedback">${error}</div>
</div>
