import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  DEFAULT_SYNTH_PROFILE,
  INITIAL_ALBUMS,
  INITIAL_ARTISTS,
  INITIAL_PLAYLISTS,
  INITIAL_RECENT_PLAYS,
  INITIAL_TRACKS,
} from './data/musicLibrary';
import {
  AcrylicDensity,
  Album,
  EqualizerGains,
  EqualizerPresetName,
  MyMusicTab,
  Playlist,
  RecentPlayEntry,
  RepeatMode,
  ScreenRoute,
  Track,
} from './types/music';
import { audioEngine } from './audio/AudioEngine';
import {
  createArtDesignForKey,
  makeSlugId,
  parseAudioFileMetadata,
} from './utils/id3Parser';
import {
  ArtistInternetData,
  fetchArtistImageFromInternet,
  getAllCachedArtistData,
} from './utils/artistImageFetcher';
import {
  StoredTrackRecord,
  buildCatalogFromTracks,
  clearAllTracksFromIDB,
  createTrackIdFromTags,
  loadTracksFromIDB,
  saveTrackRecordsToIDB,
} from './utils/libraryStorage';
import { TitleBar } from './components/TitleBar';
import { Sidebar } from './components/Sidebar';
import { PlayerBar } from './components/PlayerBar';
import { MiniPlayer } from './components/MiniPlayer';
import { MyMusicScreen } from './components/screens/MyMusicScreen';
import { AlbumDetailScreen } from './components/screens/AlbumDetailScreen';
import { ArtistDetailScreen } from './components/screens/ArtistDetailScreen';
import { RecentPlaysScreen } from './components/screens/RecentPlaysScreen';
import { NowPlayingScreen } from './components/screens/NowPlayingScreen';
import { PlaylistScreen } from './components/screens/PlaylistScreen';
import { SearchScreen } from './components/screens/SearchScreen';
import { SettingsScreen } from './components/screens/SettingsScreen';

const EQ_PRESET_MAP: Record<EqualizerPresetName, EqualizerGains> = {
  Flat: { low: 0, mid: 0, high: 0 },
  Warm: { low: 3, mid: 1, high: -2 },
  'Bass Boost': { low: 7, mid: 0, high: 1 },
  Vocal: { low: -2, mid: 5, high: 3 },
  Electronic: { low: 5, mid: -1, high: 4 },
};

const STORAGE_KEY = 'fluent-groove-state-v2';

interface SavedPrefs {
  playlists?: Playlist[];
  recentPlays?: RecentPlayEntry[];
  acrylicDensity?: AcrylicDensity;
  eqPreset?: EqualizerPresetName;
}

function loadSavedPrefs(): SavedPrefs {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as SavedPrefs) : {};
  } catch {
    return {};
  }
}

function isSupportedAudioFile(file: File): boolean {
  if (file.type && file.type.startsWith('audio/')) return true;
  return /\.(mp3|flac|m4a|mp4|wav|ogg|aac|opus|wma|webm)$/i.test(file.name);
}

export default function App() {
  const saved = useMemo(() => loadSavedPrefs(), []);

  // Library state (populated strictly from user's audio files and ID3 tags)
  const [tracks, setTracks] = useState<Track[]>(INITIAL_TRACKS);
  const [artistInternetMap, setArtistInternetMap] = useState<
    Record<string, ArtistInternetData>
  >(() => getAllCachedArtistData());
  const [playlists, setPlaylists] = useState<Playlist[]>(
    saved.playlists || INITIAL_PLAYLISTS
  );
  const [recentPlays, setRecentPlays] = useState<RecentPlayEntry[]>(
    saved.recentPlays || INITIAL_RECENT_PLAYS
  );
  const [isImporting, setIsImporting] = useState(false);
  const [importingCount, setImportingCount] = useState(0);

  // Automatically derive organized Albums (with album covers) and Artists (with internet artist photos)
  const { albums, artists } = useMemo(() => {
    if (tracks.length === 0) {
      return { albums: INITIAL_ALBUMS, artists: INITIAL_ARTISTS };
    }
    return buildCatalogFromTracks(tracks, artistInternetMap);
  }, [tracks, artistInternetMap]);

  // Automatically search the internet for artist photos whenever new artists appear in tracks
  useEffect(() => {
    if (tracks.length === 0) return;
    let cancelled = false;

    const uniqueArtistNames = Array.from(
      new Set(tracks.map((t) => t.artist.trim()).filter(Boolean))
    );

    void (async () => {
      for (const artistName of uniqueArtistNames) {
        if (cancelled) break;
        const key = artistName.toLowerCase();
        if (
          key === 'unknown artist' ||
          key === 'local audio' ||
          artistInternetMap[key]?.imageUrl
        ) {
          continue;
        }
        const found = await fetchArtistImageFromInternet(artistName);
        if (found && !cancelled) {
          setArtistInternetMap((prev) => ({
            ...prev,
            [key]: found,
          }));
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [tracks, artistInternetMap]);

  // Navigation & shell UI state
  const [sidebarExpanded, setSidebarExpanded] = useState(true);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const mobileSearchInputRef = useRef<HTMLInputElement | null>(null);
  const [route, setRoute] = useState<ScreenRoute>({
    view: 'my-music',
    tab: 'albums',
  });
  const [previousRoute, setPreviousRoute] = useState<ScreenRoute>({
    view: 'my-music',
    tab: 'albums',
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [acrylicDensity, setAcrylicDensity] = useState<AcrylicDensity>(
    saved.acrylicDensity || 'standard'
  );
  const [showTitleBar, setShowTitleBar] = useState(true);
  const [isMiniPlayer, setIsMiniPlayer] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Playback state
  const [queue, setQueue] = useState<Track[]>([]);
  const [currentQueueIndex, setCurrentQueueIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const [volume, setVolume] = useState(0.78);
  const [isMuted, setIsMuted] = useState(false);
  const [isShuffled, setIsShuffled] = useState(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>('all');
  const [eqPreset, setEqPreset] = useState<EqualizerPresetName>(
    saved.eqPreset || 'Flat'
  );
  const [eqGains, setEqGains] = useState<EqualizerGains>(
    EQ_PRESET_MAP[saved.eqPreset || 'Flat']
  );

  const currentTrack = queue[currentQueueIndex] || null;

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
  }, []);

  useEffect(() => {
    if (!toastMessage) return;
    const t = window.setTimeout(() => setToastMessage(null), 2600);
    return () => window.clearTimeout(t);
  }, [toastMessage]);

  // Load previously imported tracks & ID3 tags from IndexedDB on startup
  useEffect(() => {
    let active = true;
    void loadTracksFromIDB().then((storedTracks) => {
      if (active && storedTracks.length > 0) {
        setTracks(storedTracks);
      }
    });
    return () => {
      active = false;
    };
  }, []);

  // Persist user playlists, recent plays, and preferences
  useEffect(() => {
    try {
      const payload: SavedPrefs = {
        playlists,
        recentPlays: recentPlays.slice(0, 40),
        acrylicDensity,
        eqPreset,
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    } catch {
      // Ignore storage quota errors
    }
  }, [playlists, recentPlays, acrylicDensity, eqPreset]);

  const handleRefreshArtistImage = useCallback(
    async (artistName: string) => {
      const found = await fetchArtistImageFromInternet(artistName, true);
      if (found) {
        setArtistInternetMap((prev) => ({
          ...prev,
          [artistName.trim().toLowerCase()]: found,
        }));
        showToast(`Updated photo for ${artistName}`);
      } else {
        showToast(`Could not find an online photo for ${artistName}`);
      }
    },
    [showToast]
  );

  const recordRecentPlay = useCallback((track: Track) => {
    setRecentPlays((prev) => {
      const filtered = prev.filter(
        (entry, i) => !(i === 0 && entry.trackId === track.id)
      );
      return [
        {
          id: `rp-${Date.now()}-${Math.random()}`,
          trackId: track.id,
          playedAt: Date.now(),
        },
        ...filtered,
      ].slice(0, 40);
    });
  }, []);

  const startPlaybackAt = useCallback(
    (newQueue: Track[], index: number) => {
      if (newQueue.length === 0) return;
      const safeIdx = Math.max(0, Math.min(newQueue.length - 1, index));
      const track = newQueue[safeIdx];
      setQueue(newQueue);
      setCurrentQueueIndex(safeIdx);
      setElapsed(0);
      setIsPlaying(true);
      audioEngine.startTrack(track, 0);
      recordRecentPlay(track);
    },
    [recordRecentPlay]
  );

  const handlePlayTrack = useCallback(
    (track: Track, contextTracks: Track[]) => {
      const idx = contextTracks.findIndex((t) => t.id === track.id);
      if (idx >= 0) {
        startPlaybackAt(contextTracks, idx);
      } else {
        startPlaybackAt([track, ...contextTracks], 0);
      }
    },
    [startPlaybackAt]
  );

  const handlePlayAlbum = useCallback(
    (album: Album) => {
      const albumTracks = tracks
        .filter((t) => t.albumId === album.id)
        .sort((a, b) => a.trackNumber - b.trackNumber);
      if (albumTracks.length > 0) {
        startPlaybackAt(albumTracks, 0);
      }
    },
    [tracks, startPlaybackAt]
  );

  const handlePlayArtist = useCallback(
    (artistId: string) => {
      const artistTracks = tracks
        .filter((t) => t.artistId === artistId)
        .sort(
          (a, b) =>
            a.album.localeCompare(b.album) || a.trackNumber - b.trackNumber
        );
      if (artistTracks.length > 0) {
        startPlaybackAt(artistTracks, 0);
      }
    },
    [tracks, startPlaybackAt]
  );

  const handleShuffleList = useCallback(
    (sourceTracks: Track[]) => {
      if (sourceTracks.length === 0) return;
      const copy = [...sourceTracks];
      for (let i = copy.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [copy[i], copy[j]] = [copy[j], copy[i]];
      }
      setIsShuffled(true);
      startPlaybackAt(copy, 0);
    },
    [startPlaybackAt]
  );

  const handleTogglePlay = useCallback(() => {
    if (!currentTrack) {
      if (tracks.length > 0) {
        startPlaybackAt(tracks, 0);
      }
      return;
    }

    if (isPlaying) {
      audioEngine.pause();
      setIsPlaying(false);
    } else {
      audioEngine.resume(currentTrack, elapsed);
      setIsPlaying(true);
    }
  }, [currentTrack, isPlaying, elapsed, tracks, startPlaybackAt]);

  const handleNext = useCallback(() => {
    if (queue.length === 0) return;
    if (repeatMode === 'one') {
      startPlaybackAt(queue, currentQueueIndex);
      return;
    }
    if (isShuffled && queue.length > 1) {
      let nextIdx = currentQueueIndex;
      while (nextIdx === currentQueueIndex) {
        nextIdx = Math.floor(Math.random() * queue.length);
      }
      startPlaybackAt(queue, nextIdx);
      return;
    }
    if (currentQueueIndex + 1 < queue.length) {
      startPlaybackAt(queue, currentQueueIndex + 1);
    } else if (repeatMode === 'all') {
      startPlaybackAt(queue, 0);
    } else {
      audioEngine.pause();
      setIsPlaying(false);
      setElapsed(0);
    }
  }, [queue, currentQueueIndex, repeatMode, isShuffled, startPlaybackAt]);

  const handlePrevious = useCallback(() => {
    if (queue.length === 0) return;
    if (elapsed > 4) {
      setElapsed(0);
      if (isPlaying && currentTrack) {
        audioEngine.startTrack(currentTrack, 0);
      }
      return;
    }
    if (currentQueueIndex > 0) {
      startPlaybackAt(queue, currentQueueIndex - 1);
    } else if (repeatMode === 'all') {
      startPlaybackAt(queue, queue.length - 1);
    } else {
      setElapsed(0);
    }
  }, [
    queue,
    currentQueueIndex,
    elapsed,
    isPlaying,
    currentTrack,
    repeatMode,
    startPlaybackAt,
  ]);

  const handleSeek = useCallback((seconds: number) => {
    setElapsed(seconds);
    audioEngine.seek(seconds);
  }, []);

  // Playback clock tick
  useEffect(() => {
    if (!isPlaying || !currentTrack) return;
    const interval = window.setInterval(() => {
      setElapsed((prev) => {
        if (prev + 1 >= currentTrack.duration) {
          handleNext();
          return 0;
        }
        return prev + 1;
      });
    }, 1000);
    return () => window.clearInterval(interval);
  }, [isPlaying, currentTrack, handleNext]);

  // Global Spacebar play/pause shortcut
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.tagName === 'SELECT')
      ) {
        return;
      }
      if (e.code === 'Space') {
        e.preventDefault();
        handleTogglePlay();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [handleTogglePlay]);

  const handleVolumeChange = (val: number) => {
    setVolume(val);
    setIsMuted(val === 0);
    audioEngine.setVolume(val, val === 0);
  };

  const handleToggleMute = () => {
    const nextMuted = !isMuted;
    setIsMuted(nextMuted);
    audioEngine.setVolume(volume, nextMuted);
  };

  const handleCycleRepeat = () => {
    setRepeatMode((prev) =>
      prev === 'off' ? 'all' : prev === 'all' ? 'one' : 'off'
    );
  };

  const handleSelectEqPreset = (preset: EqualizerPresetName) => {
    setEqPreset(preset);
    const gains = EQ_PRESET_MAP[preset];
    setEqGains(gains);
    audioEngine.setEqualizer(gains);
    showToast(`Equalizer: ${preset}`);
  };

  const handleChangeEqGains = (gains: EqualizerGains) => {
    setEqGains(gains);
    audioEngine.setEqualizer(gains);
  };

  // Navigation helpers
  const navigateTo = (next: ScreenRoute) => {
    if (route.view !== 'search') {
      setPreviousRoute(route);
    }
    setRoute(next);
  };

  const handleSearchChange = (q: string) => {
    setSearchQuery(q);
    if (q.trim().length > 0) {
      if (route.view !== 'search') {
        setPreviousRoute(route);
      }
      setRoute({ view: 'search', query: q });
    } else if (route.view === 'search') {
      setRoute(previousRoute);
    }
  };

  const handleOpenMobileSearch = () => {
    setMobileMenuOpen(true);
    window.setTimeout(() => {
      mobileSearchInputRef.current?.focus();
    }, 60);
  };

  // Playlist & queue mutations
  const handleCreatePlaylist = (
    name: string,
    initialTrackIds: string[] = []
  ) => {
    const newPlaylist: Playlist = {
      id: `pl-${Date.now()}`,
      name,
      description: 'Custom playlist',
      trackIds: initialTrackIds,
      createdAt: Date.now(),
    };
    setPlaylists((prev) => [...prev, newPlaylist]);
    setRoute({ view: 'playlist', playlistId: newPlaylist.id });
    showToast(`Created playlist "${name}"`);
  };

  const handleAddToPlaylist = (playlistId: string, trackId: string) => {
    const pl = playlists.find((p) => p.id === playlistId);
    setPlaylists((prev) =>
      prev.map((p) =>
        p.id === playlistId && !p.trackIds.includes(trackId)
          ? { ...p, trackIds: [...p.trackIds, trackId] }
          : p
      )
    );
    if (pl) showToast(`Added to "${pl.name}"`);
  };

  const handleAddAlbumToPlaylist = (
    playlistId: string,
    albumTracks: Track[]
  ) => {
    const pl = playlists.find((p) => p.id === playlistId);
    const ids = albumTracks.map((t) => t.id);
    setPlaylists((prev) =>
      prev.map((p) => {
        if (p.id !== playlistId) return p;
        const merged = Array.from(new Set([...p.trackIds, ...ids]));
        return { ...p, trackIds: merged };
      })
    );
    if (pl) showToast(`Added ${ids.length} songs to "${pl.name}"`);
  };

  const handleRemoveFromPlaylist = (playlistId: string, trackId: string) => {
    setPlaylists((prev) =>
      prev.map((p) =>
        p.id === playlistId
          ? { ...p, trackIds: p.trackIds.filter((id) => id !== trackId) }
          : p
      )
    );
  };

  const handleRenamePlaylist = (playlistId: string, newName: string) => {
    setPlaylists((prev) =>
      prev.map((p) => (p.id === playlistId ? { ...p, name: newName } : p))
    );
  };

  const handleDeletePlaylist = (playlistId: string) => {
    setPlaylists((prev) => prev.filter((p) => p.id !== playlistId));
    navigateTo({ view: 'my-music', tab: 'albums' });
    showToast('Playlist deleted');
  };

  const handleAddToQueue = (track: Track) => {
    setQueue((prev) => [...prev, track]);
    showToast(`Added "${track.title}" to queue`);
  };

  const handleAddAlbumToQueue = (album: Album) => {
    const albumTracks = tracks
      .filter((t) => t.albumId === album.id)
      .sort((a, b) => a.trackNumber - b.trackNumber);
    setQueue((prev) => [...prev, ...albumTracks]);
    showToast(`Added "${album.title}" to queue`);
  };

  const handleMoveQueueItem = (fromIdx: number, toIdx: number) => {
    if (toIdx < 0 || toIdx >= queue.length) return;
    setQueue((prev) => {
      const next = [...prev];
      const [item] = next.splice(fromIdx, 1);
      next.splice(toIdx, 0, item);
      return next;
    });
    if (currentQueueIndex === fromIdx) {
      setCurrentQueueIndex(toIdx);
    } else if (fromIdx < currentQueueIndex && toIdx >= currentQueueIndex) {
      setCurrentQueueIndex((i) => i - 1);
    } else if (fromIdx > currentQueueIndex && toIdx <= currentQueueIndex) {
      setCurrentQueueIndex((i) => i + 1);
    }
  };

  const handleRemoveQueueItem = (idx: number) => {
    setQueue((prev) => prev.filter((_, i) => i !== idx));
    if (idx < currentQueueIndex) {
      setCurrentQueueIndex((i) => Math.max(0, i - 1));
    } else if (idx === currentQueueIndex) {
      setElapsed(0);
    }
  };

  // Import local audio files, read ID3/FLAC/M4A tags, and organize by Artist & Album
  const handleImportFiles = async (fileList: FileList) => {
    const audioFiles = Array.from(fileList).filter(isSupportedAudioFile);
    if (audioFiles.length === 0) {
      showToast('No supported audio files found');
      return;
    }

    setIsImporting(true);
    setImportingCount(audioFiles.length);

    const readDuration = (url: string): Promise<number> =>
      new Promise((resolve) => {
        const probe = new Audio();
        probe.preload = 'metadata';
        probe.src = url;
        const timer = window.setTimeout(() => resolve(180), 3000);
        probe.addEventListener('loadedmetadata', () => {
          window.clearTimeout(timer);
          resolve(
            Number.isFinite(probe.duration) && probe.duration > 0
              ? Math.round(probe.duration)
              : 180
          );
        });
        probe.addEventListener('error', () => {
          window.clearTimeout(timer);
          resolve(180);
        });
      });

    const newTracks: Track[] = [];
    const recordsToSave: StoredTrackRecord[] = [];
    const baseTime = Date.now();

    for (let i = 0; i < audioFiles.length; i++) {
      const file = audioFiles[i];
      const url = URL.createObjectURL(file);
      const [duration, parsed] = await Promise.all([
        readDuration(url),
        parseAudioFileMetadata(file),
      ]);

      const artistId = makeSlugId('art', parsed.artist);
      const albumId = makeSlugId('alb', `${parsed.artist}-${parsed.album}`);
      const trackId = createTrackIdFromTags(
        parsed.artist,
        parsed.album,
        parsed.title,
        parsed.trackNumber
      );
      const artDesign = createArtDesignForKey(
        `${parsed.artist}::${parsed.album}`,
        parsed.coverUrl
      );

      const trackObj: Track = {
        id: trackId,
        title: parsed.title,
        artist: parsed.artist,
        artistId,
        album: parsed.album,
        albumId,
        trackNumber: parsed.trackNumber,
        duration,
        genre: parsed.genre,
        year: parsed.year,
        addedAt: baseTime + i,
        fileName: file.name,
        artDesign,
        synth: DEFAULT_SYNTH_PROFILE,
        localUrl: url,
      };

      newTracks.push(trackObj);
      recordsToSave.push({
        id: trackId,
        title: parsed.title,
        artist: parsed.artist,
        artistId,
        album: parsed.album,
        albumId,
        trackNumber: parsed.trackNumber,
        duration,
        genre: parsed.genre,
        year: parsed.year,
        addedAt: baseTime + i,
        fileName: file.name,
        audioBlob: file,
      });
    }

    void saveTrackRecordsToIDB(recordsToSave);

    setTracks((prev) => {
      const map = new Map<string, Track>();
      for (const existing of prev) {
        map.set(existing.id, existing);
      }
      for (const incoming of newTracks) {
        map.set(incoming.id, incoming);
      }
      return Array.from(map.values()).sort((a, b) => b.addedAt - a.addedAt);
    });

    setIsImporting(false);
    setImportingCount(0);

    const uniqueArtists = new Set(newTracks.map((t) => t.artistId)).size;
    const uniqueAlbums = new Set(newTracks.map((t) => t.albumId)).size;
    showToast(
      `Organized ${newTracks.length} ${
        newTracks.length === 1 ? 'song' : 'songs'
      } (${uniqueAlbums} ${
        uniqueAlbums === 1 ? 'album' : 'albums'
      }, ${uniqueArtists} ${uniqueArtists === 1 ? 'artist' : 'artists'})`
    );
  };

  const handleToggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen?.().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen?.().catch(() => {});
      setIsFullscreen(false);
    }
  };

  const handleResetLibrary = () => {
    audioEngine.pause();
    setIsPlaying(false);
    setTracks([]);
    setPlaylists([]);
    setRecentPlays([]);
    setQueue([]);
    setCurrentQueueIndex(0);
    setElapsed(0);
    localStorage.removeItem(STORAGE_KEY);
    void clearAllTracksFromIDB();
    showToast('Library & playlists cleared');
  };

  if (isMiniPlayer) {
    return (
      <MiniPlayer
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        elapsed={elapsed}
        isShuffled={isShuffled}
        repeatMode={repeatMode}
        onTogglePlay={handleTogglePlay}
        onNext={handleNext}
        onPrevious={handlePrevious}
        onSeek={handleSeek}
        onToggleShuffle={() => setIsShuffled((s) => !s)}
        onCycleRepeat={handleCycleRepeat}
        onRestoreFull={() => setIsMiniPlayer(false)}
      />
    );
  }

  return (
    <div
      onDragOver={(e) => e.preventDefault()}
      onDrop={(e) => {
        e.preventDefault();
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
          void handleImportFiles(e.dataTransfer.files);
        }
      }}
      className="relative h-dvh w-screen flex flex-col overflow-hidden bg-white"
    >
      {/* Subtle atmospheric cyan/sky light field behind desktop left acrylic rail */}
      <div
        aria-hidden="true"
        className="hidden md:block pointer-events-none fixed inset-y-0 left-0 w-[260px] bg-gradient-to-br from-[#bfe7f4] via-[#7ec4dc] to-[#56a8c7] opacity-85 z-0"
      />

      {/* Top Desktop Chrome / Mobile Acrylic Header */}
      {showTitleBar && (
        <TitleBar
          sidebarExpanded={sidebarExpanded}
          acrylicDensity={acrylicDensity}
          isFullscreen={isFullscreen}
          onToggleFullscreen={handleToggleFullscreen}
          onEnterMiniPlayer={() => setIsMiniPlayer(true)}
          onOpenMobileMenu={() => setMobileMenuOpen(true)}
          onOpenMobileSearch={handleOpenMobileSearch}
        />
      )}

      {/* Middle Shell: Acrylic Sidebar (Slide-over on mobile, Fixed rail on desktop) + Pure White Content Canvas */}
      <div className="relative z-10 flex flex-1 min-h-0">
        <Sidebar
          expanded={sidebarExpanded}
          onToggleExpanded={() => setSidebarExpanded((prev) => !prev)}
          mobileOpen={mobileMenuOpen}
          onCloseMobile={() => setMobileMenuOpen(false)}
          searchInputRef={mobileSearchInputRef}
          route={route}
          onNavigate={(next) => {
            setSearchQuery('');
            navigateTo(next);
          }}
          playlists={playlists}
          onCreatePlaylist={(name) => handleCreatePlaylist(name)}
          searchQuery={searchQuery}
          onSearchChange={handleSearchChange}
          onImportFiles={(files) => void handleImportFiles(files)}
          acrylicDensity={acrylicDensity}
        />

        <main className="flex-1 bg-white min-w-0 overflow-y-auto fluent-scroll">
          {route.view === 'my-music' && (
            <MyMusicScreen
              activeTab={route.tab}
              onChangeTab={(tab: MyMusicTab) =>
                navigateTo({ view: 'my-music', tab })
              }
              albums={albums}
              artists={artists}
              tracks={tracks}
              playlists={playlists}
              currentTrackId={currentTrack?.id || null}
              isPlaying={isPlaying}
              isImporting={isImporting}
              importingCount={importingCount}
              onPlayAlbum={handlePlayAlbum}
              onPlayArtist={handlePlayArtist}
              onPlayTrack={handlePlayTrack}
              onTogglePlay={handleTogglePlay}
              onShuffleAll={handleShuffleList}
              onOpenAlbum={(albumId) =>
                navigateTo({ view: 'album', albumId })
              }
              onOpenArtist={(artistId) =>
                navigateTo({ view: 'artist', artistId })
              }
              onAddToQueue={handleAddToQueue}
              onAddAlbumToQueue={handleAddAlbumToQueue}
              onAddToPlaylist={handleAddToPlaylist}
              onImportFiles={(files) => void handleImportFiles(files)}
            />
          )}

          {route.view === 'album' &&
            (() => {
              const alb = albums.find((a) => a.id === route.albumId);
              if (!alb) {
                return (
                  <div className="p-6 text-[13px] text-[#666666]">
                    Album not found.
                  </div>
                );
              }
              const albTracks = tracks
                .filter((t) => t.albumId === alb.id)
                .sort((a, b) => a.trackNumber - b.trackNumber);
              return (
                <AlbumDetailScreen
                  album={alb}
                  tracks={albTracks}
                  playlists={playlists}
                  currentTrackId={currentTrack?.id || null}
                  isPlaying={isPlaying}
                  onBack={() =>
                    navigateTo({ view: 'my-music', tab: 'albums' })
                  }
                  onPlayAlbum={handlePlayAlbum}
                  onShuffleAlbum={handleShuffleList}
                  onPlayTrack={handlePlayTrack}
                  onTogglePlay={handleTogglePlay}
                  onAddToQueue={handleAddToQueue}
                  onAddAlbumToQueue={handleAddAlbumToQueue}
                  onAddToPlaylist={handleAddToPlaylist}
                  onAddAlbumToPlaylist={handleAddAlbumToPlaylist}
                  onOpenArtist={(artistId) =>
                    navigateTo({ view: 'artist', artistId })
                  }
                />
              );
            })()}

          {route.view === 'artist' &&
            (() => {
              const art = artists.find((a) => a.id === route.artistId);
              if (!art) {
                return (
                  <div className="p-6 text-[13px] text-[#666666]">
                    Artist not found.
                  </div>
                );
              }
              const artAlbums = albums.filter((a) => a.artistId === art.id);
              const artTracks = tracks.filter((t) => t.artistId === art.id);
              return (
                <ArtistDetailScreen
                  artist={art}
                  albums={artAlbums}
                  tracks={artTracks}
                  playlists={playlists}
                  currentTrackId={currentTrack?.id || null}
                  isPlaying={isPlaying}
                  onBack={() =>
                    navigateTo({ view: 'my-music', tab: 'artists' })
                  }
                  onPlayAll={(list) => startPlaybackAt(list, 0)}
                  onShuffleAll={handleShuffleList}
                  onPlayAlbum={handlePlayAlbum}
                  onOpenAlbum={(albumId) =>
                    navigateTo({ view: 'album', albumId })
                  }
                  onPlayTrack={handlePlayTrack}
                  onTogglePlay={handleTogglePlay}
                  onAddToQueue={handleAddToQueue}
                  onAddToPlaylist={handleAddToPlaylist}
                  onRefreshArtistImage={handleRefreshArtistImage}
                />
              );
            })()}

          {route.view === 'recent-plays' && (
            <RecentPlaysScreen
              recentPlays={recentPlays}
              tracks={tracks}
              albums={albums}
              playlists={playlists}
              currentTrackId={currentTrack?.id || null}
              isPlaying={isPlaying}
              onPlayAlbum={handlePlayAlbum}
              onPlayTrack={handlePlayTrack}
              onTogglePlay={handleTogglePlay}
              onOpenAlbum={(albumId) =>
                navigateTo({ view: 'album', albumId })
              }
              onOpenArtist={(artistId) =>
                navigateTo({ view: 'artist', artistId })
              }
            />
          )}

          {route.view === 'now-playing' && (
            <NowPlayingScreen
              currentTrack={currentTrack}
              isPlaying={isPlaying}
              elapsed={elapsed}
              isShuffled={isShuffled}
              repeatMode={repeatMode}
              queue={queue}
              currentQueueIndex={currentQueueIndex}
              playlists={playlists}
              onTogglePlay={handleTogglePlay}
              onNext={handleNext}
              onPrevious={handlePrevious}
              onSeek={handleSeek}
              onToggleShuffle={() => setIsShuffled((s) => !s)}
              onCycleRepeat={handleCycleRepeat}
              onJumpToQueueIndex={(idx) => startPlaybackAt(queue, idx)}
              onMoveQueueItem={handleMoveQueueItem}
              onRemoveQueueItem={handleRemoveQueueItem}
              onClearQueue={() => {
                audioEngine.pause();
                setIsPlaying(false);
                setQueue([]);
                setCurrentQueueIndex(0);
                setElapsed(0);
              }}
              onSaveQueueAsPlaylist={(name) =>
                handleCreatePlaylist(
                  name,
                  queue.map((t) => t.id)
                )
              }
              onAddToPlaylist={handleAddToPlaylist}
              onOpenAlbum={(albumId) =>
                navigateTo({ view: 'album', albumId })
              }
              onOpenArtist={(artistId) =>
                navigateTo({ view: 'artist', artistId })
              }
            />
          )}

          {route.view === 'playlist' &&
            (() => {
              const pl = playlists.find((p) => p.id === route.playlistId);
              if (!pl) {
                return (
                  <div className="p-6 text-[13px] text-[#666666]">
                    Playlist not found.
                  </div>
                );
              }
              return (
                <PlaylistScreen
                  playlist={pl}
                  allTracks={tracks}
                  playlists={playlists}
                  currentTrackId={currentTrack?.id || null}
                  isPlaying={isPlaying}
                  onPlayPlaylist={(list) => startPlaybackAt(list, 0)}
                  onShufflePlaylist={handleShuffleList}
                  onPlayTrack={handlePlayTrack}
                  onTogglePlay={handleTogglePlay}
                  onAddToQueue={handleAddToQueue}
                  onAddToPlaylist={handleAddToPlaylist}
                  onRemoveFromPlaylist={handleRemoveFromPlaylist}
                  onRenamePlaylist={handleRenamePlaylist}
                  onDeletePlaylist={handleDeletePlaylist}
                  onOpenAlbum={(albumId) =>
                    navigateTo({ view: 'album', albumId })
                  }
                  onOpenArtist={(artistId) =>
                    navigateTo({ view: 'artist', artistId })
                  }
                />
              );
            })()}

          {route.view === 'search' && (
            <SearchScreen
              query={route.query}
              tracks={tracks}
              albums={albums}
              artists={artists}
              playlists={playlists}
              currentTrackId={currentTrack?.id || null}
              isPlaying={isPlaying}
              onPlayAlbum={handlePlayAlbum}
              onPlayTrack={handlePlayTrack}
              onTogglePlay={handleTogglePlay}
              onAddToQueue={handleAddToQueue}
              onAddToPlaylist={handleAddToPlaylist}
              onOpenAlbum={(albumId) =>
                navigateTo({ view: 'album', albumId })
              }
              onOpenArtist={(artistId) =>
                navigateTo({ view: 'artist', artistId })
              }
              onOpenPlaylist={(playlistId) =>
                navigateTo({ view: 'playlist', playlistId })
              }
            />
          )}

          {route.view === 'settings' && (
            <SettingsScreen
              acrylicDensity={acrylicDensity}
              onChangeAcrylicDensity={setAcrylicDensity}
              eqPreset={eqPreset}
              eqGains={eqGains}
              onSelectEqPreset={handleSelectEqPreset}
              onChangeEqGains={handleChangeEqGains}
              showTitleBar={showTitleBar}
              onToggleTitleBar={() => setShowTitleBar((v) => !v)}
              trackCount={tracks.length}
              albumCount={albums.length}
              playlistCount={playlists.length}
              onImportFiles={(files) => void handleImportFiles(files)}
              onResetLibrary={handleResetLibrary}
            />
          )}
        </main>
      </div>

      {/* Subtle Crisp Toast Notification */}
      {toastMessage && (
        <div
          role="status"
          className="fixed right-4 md:right-5 bottom-[74px] md:bottom-[84px] z-[110] bg-[#1c1b1b] text-white px-4 py-2 text-[12px] border border-white/15 shadow-md rounded-none"
        >
          {toastMessage}
        </div>
      )}

      {/* Bottom Persistent Accent Blue Transport Deck (Minimalist on Mobile, Full 3-Zone on Desktop) */}
      <PlayerBar
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        elapsed={elapsed}
        volume={volume}
        isMuted={isMuted}
        isShuffled={isShuffled}
        repeatMode={repeatMode}
        eqPreset={eqPreset}
        onTogglePlay={handleTogglePlay}
        onNext={handleNext}
        onPrevious={handlePrevious}
        onSeek={handleSeek}
        onVolumeChange={handleVolumeChange}
        onToggleMute={handleToggleMute}
        onToggleShuffle={() => setIsShuffled((s) => !s)}
        onCycleRepeat={handleCycleRepeat}
        onOpenNowPlaying={() => navigateTo({ view: 'now-playing' })}
        onOpenArtist={(artistId) => navigateTo({ view: 'artist', artistId })}
        onSelectEqPreset={handleSelectEqPreset}
        onEnterMiniPlayer={() => setIsMiniPlayer(true)}
      />
    </div>
  );
}
