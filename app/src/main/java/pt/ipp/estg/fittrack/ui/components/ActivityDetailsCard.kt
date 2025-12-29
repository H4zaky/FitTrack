package pt.ipp.estg.fittrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ActivityDetailsUi(
    val title: String,
    val subtitle: String,
    val distanceKm: Double? = null,
    val durationMin: Int? = null
)

@Composable
fun ActivityDetailsCard(
    details: ActivityDetailsUi?,
    modifier: Modifier = Modifier
) {
    if (details == null) return

    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(details.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(details.subtitle, style = MaterialTheme.typography.bodyMedium)

            details.distanceKm?.let {
                Spacer(Modifier.height(8.dp))
                Text("Distância: %.2f km".format(it))
            }
            details.durationMin?.let {
                Text("Duração: ${it} min")
            }
        }
    }
}
