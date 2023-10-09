/*
 * Album Store API
 *
 * CS6650 Fall 2023
 *
 * API version: 1.0.0
 * Contact: i.gorton@northeasern.edu
 * Generated by: Swagger Codegen (https://github.com/swagger-api/swagger-codegen.git)
 */
package swagger

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"sync"

	"github.com/gorilla/mux"
)

var myMutex = &sync.Mutex{}
var albumStore = make(map[string]Album)     // In-memory store for albums
var profileStore = make(map[string]Profile) // In-memory store for profiles

type Album struct {
	AlbumID   string  `json:"albumId"`
	Image     []byte  `json:"-"`
	ImageSize int     `json:"imageSize"`
	Profile   Profile `json:"profile"`
}

type Profile struct {
	Artist string `json:"artist"`
	Title  string `json:"title"`
	Year   string `json:"year"`
}

func GetAlbumByKey(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")

	vars := mux.Vars(r)
	albumID := vars["albumID"]

	myMutex.Lock()
	profile, ok := profileStore[albumID]
	myMutex.Unlock()
	if !ok {
		http.Error(w, "Unable to find profile", http.StatusBadRequest)
		return
	}

	response, err := json.Marshal(profile)
	if err != nil {
		http.Error(w, "Unable to serialize profile", http.StatusBadRequest)
		return
	}
	w.Write(response)
	w.WriteHeader(http.StatusOK)
}

func NewAlbum(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")

	err := r.ParseMultipartForm(10 << 20) // max memory size
	if err != nil {
		http.Error(w, "Unable to parse form", http.StatusBadRequest)
		return
	}

	file, _, err := r.FormFile("image")
	if err != nil {
		http.Error(w, "Unable to get image", http.StatusBadRequest)
		return
	}
	defer file.Close()

	imageData := make([]byte, r.ContentLength)
	_, err = file.Read(imageData)
	if err != nil && err != io.EOF {
		http.Error(w, "Unable to read image", http.StatusBadRequest)
		return
	}

	profileData := r.FormValue("profile")
	var profile Profile
	err = json.Unmarshal([]byte(profileData), &profile)
	if err != nil {
		http.Error(w, "Unable to parse profile JSON", http.StatusBadRequest)
		return
	}

	albumID := fmt.Sprintf("%d", len(albumStore)+1) // simple incrementing ID, can be improved
	album := Album{
		AlbumID:   albumID,
		Image:     imageData,
		ImageSize: len(imageData),
		Profile:   profile,
	}
	myMutex.Lock()
	albumStore[albumID] = album
	profileStore[albumID] = profile
	myMutex.Unlock()
	response := map[string]interface{}{
		"albumId":   albumID,
		"imageSize": len(imageData),
	}
	json.NewEncoder(w).Encode(response)
}

// fmt.Print("album:" + album.AlbumID + "\n")
// fmt.Println("Image size (in bytes):", len(imageData))
// w.WriteHeader(http.StatusOK)