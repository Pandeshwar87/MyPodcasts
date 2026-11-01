package com.example.mypodcasts.server

import com.example.mypodcasts.data.model.PlayListWithTracks
import com.google.gson.Gson
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD

class LocalWebServer(
    port: Int = 8080,
    private val getPlaylistsProvider: () -> List<PlayListWithTracks>,
    private val onAddTrack: (playlistId: Long, title: String, artist: String?, mediaUrl: String) -> Unit
) : NanoHTTPD(port) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                method == Method.GET && uri == "/" -> {
                    newFixedLengthResponse(Response.Status.OK, "text/html", getWebDashboardHtml())
                }
                method == Method.GET && uri == "/api/playlists" -> {
                    val playlists = getPlaylistsProvider().map { item ->
                        mapOf(
                            "id" to item.playlist.id,
                            "title" to item.playlist.title,
                            "trackCount" to item.tracks.size
                        )
                    }
                    val json = gson.toJson(playlists)
                    newFixedLengthResponse(Response.Status.OK, "application/json", json)
                }
                method == Method.POST && uri == "/api/add-track" -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val jsonBody = map["postData"] ?: ""

                    val jsonObject = gson.fromJson(jsonBody, JsonObject::class.java)
                    val playlistId = jsonObject.get("playlistId")?.asLong ?: 0L
                    val mediaUrl = jsonObject.get("mediaUrl")?.asString ?: ""
                    val title = jsonObject.get("title")?.asString ?: ""
                    val artist = jsonObject.get("artist")?.asString

                    if (playlistId > 0 && mediaUrl.isNotBlank()) {
                        onAddTrack(playlistId, title, artist, mediaUrl)
                        val responseJson = gson.toJson(mapOf("status" to "success", "message" to "Track added to phone successfully!"))
                        newFixedLengthResponse(Response.Status.OK, "application/json", responseJson)
                    } else {
                        val responseJson = gson.toJson(mapOf("status" to "error", "message" to "Invalid playlistId or mediaUrl"))
                        newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", responseJson)
                    }
                }
                else -> {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorJson = gson.toJson(mapOf("status" to "error", "message" to (e.message ?: "Server error")))
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", errorJson)
        }
    }

    private fun getWebDashboardHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MyPodcasts — Laptop Link</title>
                <style>
                    :root {
                        --bg-color: #12101D;
                        --card-bg: #1E1B2E;
                        --accent: #8E44AD;
                        --accent-hover: #9B59B6;
                        --text-primary: #FFFFFF;
                        --text-secondary: #A0A0B0;
                        --border: #2D2942;
                        --success: #2ECC71;
                        --error: #E74C3C;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-primary);
                        margin: 0;
                        padding: 24px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        box-sizing: border-box;
                    }
                    .card {
                        background-color: var(--card-bg);
                        border: 1px solid var(--border);
                        border-radius: 16px;
                        padding: 32px;
                        max-width: 520px;
                        width: 100%;
                        box-shadow: 0 8px 24px rgba(0,0,0,0.5);
                    }
                    h1 {
                        margin-top: 0;
                        font-size: 24px;
                        color: var(--text-primary);
                        display: flex;
                        align-items: center;
                        gap: 12px;
                    }
                    p.subtitle {
                        color: var(--text-secondary);
                        font-size: 14px;
                        margin-bottom: 24px;
                    }
                    .form-group {
                        margin-bottom: 18px;
                    }
                    label {
                        display: block;
                        font-size: 13px;
                        font-weight: 600;
                        color: var(--text-secondary);
                        margin-bottom: 8px;
                    }
                    input, select {
                        width: 100%;
                        padding: 12px 14px;
                        background-color: #12101D;
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        color: var(--text-primary);
                        font-size: 14px;
                        box-sizing: border-box;
                        outline: none;
                        transition: border-color 0.2s;
                    }
                    input:focus, select:focus {
                        border-color: var(--accent);
                    }
                    button {
                        width: 100%;
                        padding: 14px;
                        background-color: var(--accent);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 15px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: background-color 0.2s;
                        margin-top: 10px;
                    }
                    button:hover {
                        background-color: var(--accent-hover);
                    }
                    #statusMessage {
                        margin-top: 16px;
                        padding: 12px;
                        border-radius: 8px;
                        font-size: 14px;
                        display: none;
                        text-align: center;
                    }
                    .success { background-color: rgba(46, 204, 113, 0.15); color: var(--success); border: 1px solid var(--success); }
                    .error { background-color: rgba(231, 76, 60, 0.15); color: var(--error); border: 1px solid var(--error); }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>🎙️ MyPodcasts Laptop Link</h1>
                    <p class="subtitle">Push media links directly to your phone over local Wi-Fi.</p>

                    <div class="form-group">
                        <label for="playlistSelect">Select Target Playlist *</label>
                        <select id="playlistSelect">
                            <option value="">Loading playlists...</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="mediaUrl">URL Link (YouTube, Spotify, MP3 stream) *</label>
                        <input type="text" id="mediaUrl" placeholder="https://youtube.com/watch?v=... or https://open.spotify.com/..." required>
                    </div>

                    <div class="form-group">
                        <label for="trackTitle">Track Title (Optional)</label>
                        <input type="text" id="trackTitle" placeholder="Auto-detected if left blank">
                    </div>

                    <div class="form-group">
                        <label for="artist">Artist / Podcast Host (Optional)</label>
                        <input type="text" id="artist" placeholder="e.g. Joe Rogan, Huberman Lab">
                    </div>

                    <button id="addBtn" onclick="addTrack()">Push Track to Phone</button>

                    <div id="statusMessage"></div>
                </div>

                <script>
                    async function loadPlaylists() {
                        const select = document.getElementById('playlistSelect');
                        try {
                            const res = await fetch('/api/playlists');
                            const playlists = await res.json();
                            select.innerHTML = '';
                            if (playlists.length === 0) {
                                select.innerHTML = '<option value="">No playlists found. Create one on phone first!</option>';
                                return;
                            }
                            playlists.forEach(pl => {
                                const opt = document.createElement('option');
                                opt.value = pl.id;
                                opt.textContent = pl.title + ' (' + pl.trackCount + ' tracks)';
                                select.appendChild(opt);
                            });
                        } catch (err) {
                            select.innerHTML = '<option value="">Failed to connect to phone.</option>';
                        }
                    }

                    async function addTrack() {
                        const playlistId = document.getElementById('playlistSelect').value;
                        const mediaUrl = document.getElementById('mediaUrl').value.trim();
                        const title = document.getElementById('trackTitle').value.trim();
                        const artist = document.getElementById('artist').value.trim();
                        const status = document.getElementById('statusMessage');

                        if (!playlistId || !mediaUrl) {
                            showStatus('Please select a playlist and paste a valid URL.', 'error');
                            return;
                        }

                        document.getElementById('addBtn').disabled = true;
                        document.getElementById('addBtn').textContent = 'Pushing to phone...';

                        try {
                            const res = await fetch('/api/add-track', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    playlistId: parseInt(playlistId),
                                    mediaUrl: mediaUrl,
                                    title: title,
                                    artist: artist
                                })
                            });
                            const result = await res.json();
                            if (res.ok) {
                                showStatus('✅ Track pushed to phone successfully!', 'success');
                                document.getElementById('mediaUrl').value = '';
                                document.getElementById('trackTitle').value = '';
                                document.getElementById('artist').value = '';
                                loadPlaylists();
                            } else {
                                showStatus('❌ Error: ' + (result.message || 'Failed to add track'), 'error');
                            }
                        } catch (err) {
                            showStatus('❌ Error communicating with phone server.', 'error');
                        } finally {
                            document.getElementById('addBtn').disabled = false;
                            document.getElementById('addBtn').textContent = 'Push Track to Phone';
                        }
                    }

                    function showStatus(msg, type) {
                        const status = document.getElementById('statusMessage');
                        status.textContent = msg;
                        status.className = type;
                        status.style.display = 'block';
                    }

                    loadPlaylists();
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
