package com.ihl.webofproducts.controllers.web

import com.ihl.webofproducts.service.CatalogPulseService
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Controller
class ActivityEventController(
    private val catalogPulseService: CatalogPulseService,
) {
//    @ModelAttribute("theme")
//    fun theme(): String = "light"

    @GetMapping("/internal-events")
    fun catalogPulse(model: Model): String {
        return "activity-event"
    }

    @GetMapping("/internal-events/fragments/form")
    fun formFragment(model: Model): String {
        val eventTypeOptions = catalogPulseService.getEventTypeOptions()
        val eventSeverityOptions = catalogPulseService.getEventSeverityOptions()
        val (eventProductOptions, eventVariantOptions) = catalogPulseService.getEventProductAndVariantOptions()

        model.addAttribute("eventTypeOptions", eventTypeOptions)
        model.addAttribute("severityOptions", eventSeverityOptions)
        model.addAttribute("productOptions", eventProductOptions)
        model.addAttribute("variantOptions", eventVariantOptions)
        return "fragments/activity-event-form :: eventForm"
    }

    @GetMapping("/internal-events/fragments/metrics")
    fun metricsFragment(): String {
        return "fragments/activity-event-metrics :: metricsContent"
    }

    @GetMapping("/internal-events/fragments/feed")
    fun feedFragment(): String {
        return "fragments/activity-event-feed :: activityFeed"
    }

    @GetMapping("/internal-events/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.ALL_VALUE])
    @ResponseBody
    fun catalogPulseStream(): SseEmitter {
        return catalogPulseService.subscribeToFeedEmitter()
    }


    @PostMapping("/internal-events/events")
    fun createCatalogPulseEvent(
        @RequestParam(required = false) productInternalId: String?,
        @RequestParam(required = false) variantInternalId: String?,
        @RequestParam eventType: String,
        @RequestParam severity: String,
        @RequestParam sourceLabel: String,
        @RequestParam headline: String,
        @RequestParam detailText: String,
        @RequestParam(required = false, defaultValue = "{}") metadataJson: String,
        model: Model,
    ): String {
        catalogPulseService.createEvent(
            eventType = eventType,
            severity = severity,
            sourceLabel = sourceLabel,
            headline = headline,
            detailText = detailText,
            metadataJson = metadataJson,
            productId = productInternalId,
            variantId = variantInternalId
        )

        model.addAttribute("submissionNotice", "Event '$headline' recorded!")
        return "fragments/activity-event-form :: eventFormStatus"
    }
}