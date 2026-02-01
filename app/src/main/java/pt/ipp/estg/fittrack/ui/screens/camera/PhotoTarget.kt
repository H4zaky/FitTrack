package pt.ipp.estg.fittrack.ui.screens.camera

enum class PhotoTarget(
    val routeValue: String,
    val title: String,
    val label: String
) {
    BEFORE("before", "Foto antes", "antes"),
    AFTER("after", "Foto depois", "depois");

    companion object {
        fun fromRoute(value: String?): PhotoTarget {
            return entries.firstOrNull { it.routeValue == value } ?: BEFORE
        }
    }
}
