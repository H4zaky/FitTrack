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
    val distanceKm: Double,
    val durationMin: Int,
    val avgSpeedKmh: Double? = null,
    val elevationGainM: Double? = null,
    val steps: Long? = null,
    val start: String? = null,
    val end: String? = null,
    val weather: String? = null
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

            Spacer(Modifier.height(8.dp))
            Text("Distância: %.2f km".format(details.distanceKm))
            Text("Duração: ${details.durationMin} min")

            details.avgSpeedKmh?.let {
                Spacer(Modifier.height(8.dp))
                Text("Vel. média: %.1f km/h".format(it))
            }

            details.elevationGainM?.let { Text("Elevação: +%.0f m".format(it)) }

            details.steps?.let { Text("Passos: $it") }

            details.weather?.let {
                Spacer(Modifier.height(8.dp))
                Text("Tempo: $it")
            }

            details.start?.let {
                Spacer(Modifier.height(8.dp))
                Text("Início: $it")
            }

            details.end?.let { Text("Fim: $it") }
        }
    }
}
