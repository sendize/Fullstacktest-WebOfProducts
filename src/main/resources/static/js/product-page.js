htmx.config.defaultFocusScroll = true;

document.body.addEventListener('htmx:after:sse:message', function (evt) {
    let message = evt.detail.message

    if (message.event === "metricsChange") {
        const data = JSON.parse(message.data)

        document.getElementById("metricsProductCount").textContent = data.productCount
        document.getElementById("metricsVariantCount").textContent = data.variantCount
        document.getElementById("metricsTodayEventCount").textContent = data.todayEventCount
        document.getElementById("metricsCriticalEventCount").textContent = data.criticalEventCount
        // console.log("product count " + data.productCount)
        // console.log(message)
    }

    // console.log(evt.detail.message)
});

