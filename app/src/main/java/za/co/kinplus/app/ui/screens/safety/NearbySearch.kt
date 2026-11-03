package za.co.kinplus.app.ui.screens.safety

import java.net.URLEncoder
import java.util.Locale

/*
 * Links for the Safety hub's nearby help rows. The search is centred on the
 * user's last known position so the maps app lists places around them, not
 * around wherever it last looked. Without a fix it falls back to geo:0,0,
 * which leaves the maps app to work out where the user is.
 */

private fun coord(value: Double) = String.format(Locale.US, "%.6f", value)

private fun encodeQuery(query: String): String = URLEncoder.encode(query, "UTF-8")

/** A geo: URI that searches for [query] around [lat],[lng], e.g. geo:-25.747700,28.185800?q=police+station. */
internal fun nearbyGeoUri(query: String, lat: Double?, lng: Double?): String {
    val centre = if (lat != null && lng != null) "${coord(lat)},${coord(lng)}" else "0,0"
    return "geo:$centre?q=${encodeQuery(query)}"
}

/** Google Maps in the browser, for phones with no maps app; centred on [lat],[lng] when known. */
internal fun nearbyWebUrl(query: String, lat: Double?, lng: Double?): String {
    val search = "https://www.google.com/maps/search/${encodeQuery(query)}"
    return if (lat != null && lng != null) "$search/@${coord(lat)},${coord(lng)},14z" else search
}
