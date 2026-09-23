/* HyperOS glassmorphism weather widget. */
package com.hyperos.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

class HyperOSWeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weather = WeatherRepository.load(context)
        val now = Date()
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
        val dayName = SimpleDateFormat("EEEE", Locale("vi", "VN")).format(now)
        provideContent {
            WidgetContent(currentTime, currentDate, dayName, weather)
        }
    }

    @Composable
    private fun WidgetContent(time: String, date: String, dayName: String, weather: WeatherSnapshot) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp).background(Color(0x33444444))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, color = ColorProvider(Color.White))
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        Text(text = "$dayName $date", style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.White)))
                        Text(text = lunarDate(Date()), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFFFDE047))))
                        Text(text = lunarCanChi(Date()), style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(0xFFFEF08A))))
                    }
                }
                Spacer(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(Color(0x33FFFFFF)))
                Column(horizontalAlignment = Alignment.Start, modifier = GlanceModifier.padding(start = 8.dp)) {
                    Text(text = if (weather.city == "Cho phép vị trí") "--°C" else "${weather.temperature}°C", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Light, color = ColorProvider(Color.White)))
                    Text(text = "${weather.city}", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)))
                    Text(text = weather.description, style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(0xCCFFFFFF))))
                }
            }
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (weather.days.isEmpty()) {
                    ForecastColumn("Vị trí", "-- / --", "Cần quyền vị trí")
                } else {
                    weather.days.forEach { d -> ForecastColumn(d.day, "${d.min}° / ${d.max}°", d.description) }
                }
            }
        }
    }

    @Composable
    private fun ForecastColumn(day: String, temp: String, desc: String) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = day, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)))
            Text(text = temp, style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.White)))
            Text(text = desc, style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xAAFFFFFF))))
        }
    }

    private fun lunarDate(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        val lunar = LunarCalendar.convert(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        return "Âm lịch: ${lunar.day}/${lunar.month}"
    }

    private fun lunarCanChi(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        val lunar = LunarCalendar.convert(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        return "(${canChiYear(lunar.year)})"
    }

    private fun canChiYear(year: Int): String {
        val can = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
        val chi = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
        return "${can[(year + 6) % 10]} ${chi[(year + 8) % 12]}"
    }
}
