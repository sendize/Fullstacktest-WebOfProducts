package com.ihl.webofproducts.controllers.api

import com.ihl.webofproducts.database.model.ActivityEvent
import com.ihl.webofproducts.service.CatalogPulseService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/catalog-pulse")
class CatalogPulseApiController(
    private val catalogPulseService: CatalogPulseService,
) {

//    @GetMapping("/metrics")
//    fun metrics(): ResponseEntity<CatalogPulseMetrics> =
//        ResponseEntity.ok(catalogPulseService.currentMetrics())

    @GetMapping("/events")
    fun recentEvents(
        @RequestParam(defaultValue = "12") limit: Int,
    ): ResponseEntity<List<ActivityEvent>> =
        ResponseEntity.ok(catalogPulseService.recentEvents(limit))

//    @PostMapping("/events")
//    fun createEvent(
//        @RequestBody request: CatalogPulseCreateEventRequest,
//    ): ResponseEntity<CatalogActivityEvent> {
//        val created = catalogPulseService.createEvent(
//            CreateCatalogPulseEventCommand(
//                productInternalId = request.productInternalId,
//                variantInternalId = request.variantInternalId,
//                eventType = request.eventType,
//                severity = request.severity,
//                sourceLabel = request.sourceLabel,
//                headline = request.headline,
//                detailText = request.detailText,
//                metadataJson = request.metadataJson,
//            )
//        )
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(created)
//    }
}
