package main

import (
	"Observer/api"
	v1 "Observer/api/v1"
	"Observer/db"
	"Observer/event_broadcast"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
)

func main() {
	godotenv.Load()

	r := gin.Default()
	api.RegisterRoutes(r)
	v1.RegisterRoutes(r.Group("/"))

	err := db.Provider.Start()
	if err != nil {
		println("ERROR: " + err.Error())
	}

	err = event_broadcast.Provider.Init()

	if err != nil {
		println("ERROR: " + err.Error())
	}

	r.Run(":8080")
}
