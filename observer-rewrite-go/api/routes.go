package api

import (
	"Observer/docs"
	"github.com/gin-gonic/gin"
	ginSwagger "github.com/swaggo/gin-swagger"
)

import "github.com/swaggo/files"         // swagger embed files
import _ "github.com/swaggo/gin-swagger" // gin-swagger middleware

// RegisterRoutes in the api package is only for generic endpoints, like health or prometheus
func RegisterRoutes(r *gin.Engine) {

	docs.SwaggerInfo.BasePath = "/"
	docs.SwaggerInfo.Description = "Observer - NetherGames Moderation API"
	docs.SwaggerInfo.Title = "Observer"
	r.GET("/health", health)

	r.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
}
