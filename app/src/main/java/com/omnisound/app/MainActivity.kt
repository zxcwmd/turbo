package com.omnisound.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnisound.app.domain.RecommendationEngine
import com.omnisound.app.domain.Track

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { com.omnisound.app.ui.theme.OmniTheme { OmniApp() } }
    }
}

private val demoTracks = listOf(
    Track("1", "afterglow", "slowdive", genre = "Dream pop", bpm = 112, tags = setOf("hazy", "guitar", "shoegaze")),
    Track("2", "the less i know", "tame impala", genre = "Psychedelic", bpm = 117, tags = setOf("indie", "bass", "dreamy")),
    Track("3", "glory box", "portishead", genre = "Trip-hop", bpm = 98, tags = setOf("dark", "downtempo", "electronic")),
    Track("4", "chamber of reflection", "mac demarco", genre = "Indie", bpm = 92, tags = setOf("dreamy", "lo-fi", "guitar")),
    Track("5", "teardrop", "massive attack", genre = "Trip-hop", bpm = 76, tags = setOf("dark", "electronic", "ambient"))
)

@Composable
private fun OmniApp() {
    var tab by remember { mutableStateOf("Home") }
    var selected by remember { mutableStateOf(demoTracks[0]) }
    var playing by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(setOf<String>()) }
    var query by remember { mutableStateOf("") }
    val engine = remember { RecommendationEngine() }
    val ranked = remember { engine.rank(demoTracks, emptyList(), selected) }
    Scaffold(
        containerColor = Color(0xFF080808),
        bottomBar = {
            Column {
                MiniPlayer(selected, playing, onToggle = { playing = !playing }, onOpen = { tab = "Player" })
                NavigationBar(containerColor = Color(0xFF0D0D0D), tonalElevation = 0.dp) {
                    listOf("Home" to Icons.Default.Home, "Radio" to Icons.Default.GraphicEq, "Search" to Icons.Default.Search, "Library" to Icons.Default.LibraryMusic).forEach { (name, icon) ->
                        NavigationBarItem(selected = tab == name, onClick = { tab = name }, icon = { Icon(icon, null) }, label = { Text(name, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = Color.White, indicatorColor = Color(0xFF292929), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                    }
                }
            }
        }
    ) { padding ->
        when (tab) {
            "Player" -> PlayerScreen(selected, playing, { playing = !playing }, { liked = if (selected.id in liked) liked - selected.id else liked + selected.id }, selected.id in liked, Modifier.padding(padding))
            "Search" -> SearchScreen(query, { query = it }, demoTracks.filter { (it.title + it.artist + it.genre).contains(query, true) }, { selected = it; tab = "Player" }, Modifier.padding(padding))
            "Radio" -> RadioScreen(selected, ranked, { selected = it; playing = true; tab = "Player" }, Modifier.padding(padding))
            "Library" -> LibraryScreen(liked.mapNotNull { id -> demoTracks.find { it.id == id } }, { selected = it; tab = "Player" }, Modifier.padding(padding))
            else -> HomeScreen(selected, ranked, { selected = it; playing = true; tab = "Player" }, { tab = "Radio" }, Modifier.padding(padding))
        }
    }
}

@Composable private fun HomeScreen(seed: Track, recommendations: List<Track>, onTrack: (Track) -> Unit, onRadio: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("omnisound", fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.4).sp); Text("YOUR SOUND, YOUR UNIVERSE", color = Color.Gray, fontSize = 9.sp, letterSpacing = 2.sp) }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF202020)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White) }
            }
        }
        item {
            Text("MONDAY MIX", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF343434), Color(0xFF111111)))).clickable { onRadio() }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A little closer\nto your taste.", fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
                    Text("A mix built from your listening patterns", color = Color.LightGray, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("PLAY RADIO", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(7.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(15.dp)) }
                }
                Box(Modifier.size(100.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF777777), Color(0xFF222222)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            }
        }
        item { SectionTitle("Made for you", "BASED ON YOUR TASTE") }
        items(recommendations) { track -> TrackRow(track, onClick = { onTrack(track) }) }
        item { SectionTitle("Start a radio", "PICK A TRACK TO EXPLORE") }
        item { TrackRow(seed, onClick = { onRadio() }, trailing = { Icon(Icons.Default.Radio, null, tint = Color.Gray) }) }
        item { Spacer(Modifier.height(8.dp)); Text("Recommendations are currently running in local demo mode. Connect your SoundCloud API credentials to load live music.", color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp) }
    }
}

@Composable private fun RadioScreen(seed: Track, tracks: List<Track>, onTrack: (Track) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp)) {
        Text("RADIO", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp)
        Text("Keep discovering.", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF191919)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(seed, 62); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("RADIO STARTED FROM", color = Color.Gray, fontSize = 9.sp, letterSpacing = 1.sp); Text(seed.title, fontWeight = FontWeight.Bold); Text(seed.artist, color = Color.Gray, fontSize = 12.sp) }; Icon(Icons.Default.GraphicEq, null)
        }
        Spacer(Modifier.height(24.dp)); Text("UP NEXT", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) { items(tracks) { TrackRow(it, onClick = { onTrack(it) }) } }
    }
}

@Composable private fun SearchScreen(query: String, onQuery: (String) -> Unit, tracks: List<Track>, onTrack: (Track) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Text("Search", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(query, onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tracks, artists, moods...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(18.dp)); Text("TRACKS", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp)
        LazyColumn { items(tracks) { TrackRow(it, onClick = { onTrack(it) }) } }
    }
}

@Composable private fun LibraryScreen(liked: List<Track>, onTrack: (Track) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Text("Your library", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("SAVED MUSIC · ON THIS DEVICE", color = Color.Gray, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(24.dp)); Text("Liked tracks", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (liked.isEmpty()) Text("Your liked tracks will appear here.", color = Color.Gray, modifier = Modifier.padding(top = 18.dp)) else LazyColumn { items(liked) { TrackRow(it, onClick = { onTrack(it) }) } }
    }
}

@Composable private fun PlayerScreen(track: Track, playing: Boolean, onToggle: () -> Unit, onLike: () -> Unit, liked: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("NOW PLAYING", fontSize = 9.sp, letterSpacing = 2.sp, color = Color.Gray); Text("SOUNDCLOUD", fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF555555), Color(0xFF141414)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GraphicEq, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(110.dp))
            }
            Spacer(Modifier.height(28.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(track.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artist, color = Color.Gray, fontSize = 14.sp) }; IconButton(onClick = onLike) { Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (liked) Color.White else Color.Gray) } }
        }
        Column(Modifier.fillMaxWidth()) {
            Slider(value = .32f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("1:24", color = Color.Gray, fontSize = 10.sp); Text("4:12", color = Color.Gray, fontSize = 10.sp) }
            Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, null, tint = Color.Gray) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                IconButton(onClick = onToggle, modifier = Modifier.size(68.dp).clip(CircleShape).background(Color.White)) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(34.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.QueueMusic, null, tint = Color.Gray) }
            }
        }
    }
}

@Composable private fun MiniPlayer(track: Track, playing: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF202020)).clickable { onOpen() }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track, 38); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(track.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1); Text(track.artist, color = Color.Gray, fontSize = 10.sp) }
        IconButton(onClick = onToggle, modifier = Modifier.size(38.dp)) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White) }
    }
}

@Composable private fun TrackRow(track: Track, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track, 52); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(track.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1) }; trailing?.invoke() ?: Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray)
    }
}

@Composable private fun Artwork(track: Track, size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Color(0xFF606060), Color(0xFF171717)))), contentAlignment = Alignment.Center) { Text(track.title.take(1).uppercase(), color = Color.White.copy(alpha = .85f), fontSize = (size / 2).sp, fontWeight = FontWeight.Black) }
}

@Composable private fun SectionTitle(title: String, eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(eyebrow, color = Color.Gray, fontSize = 9.sp, letterSpacing = 1.7.sp); Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
}
