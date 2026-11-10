@import dev.template.application.web.ui.FeatureViews.Filter

@param Filter filter

<input type="hidden" name="name" value="${filter.name()}">
<input type="hidden" name="status" value="${filter.status()}">
<input type="hidden" name="sort" value="${filter.sort().name()}">
<input type="hidden" name="descending" value="${filter.descending()}">
<input type="hidden" name="size" value="${filter.size()}">
