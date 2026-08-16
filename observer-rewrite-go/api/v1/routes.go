package v1

import "github.com/gin-gonic/gin"

func RegisterRoutes(r *gin.RouterGroup) {

	// Player
	r.POST("/player/ip", getXUIDAddresses)

	r.POST("/player/:xuid", postPointMapping)
	r.GET("/player/:xuid", getPlayerStatus)
	r.GET("/player/:xuid/trace", tracePlayer)
	r.DELETE("/player/:xuid/unlink", unlinkPlayer)
	r.GET("/player/:xuid/explain", getPointExplanation)

	// Misc

	r.PUT("/kick", kick)

	// Punishment Reasons

	r.GET("/punishment/reasons", getReasons)
	r.PUT("/punishment/reasons", insertReason)

	r.PATCH("/punishment/reasons/:name", patchReason)
	r.DELETE("/punishment/reasons/:name", deleteReason)

	// Punishments

	r.GET("/punishment/:id", getPunishmentById)
	r.PATCH("/punishment/:id", setPunishmentById)
	r.DELETE("/punishment/:id", deletePunishmentbyId)

	// Evidence

	r.PUT("/punishment/:id/evidence", uploadEvidence)
	r.GET("/punishment/:id/evidence", getEvidence)
	r.PATCH("/punishment/:id/evidence", updateEvidenceNote)
	r.DELETE("/punishment/:id/evidence", deleteEvidence)

	r.PUT("/evidence/:report_id", uploadTemporaryEvidence)
	r.GET("/evidence/:report_id", redirectTemporaryEvidence)
	r.POST("/evidence/:report_id", patchTemporaryEvidence)

	// Player bulk

	r.POST("/punishment/player", getPunishments)
	r.PUT("/punishment/player", createPunishment)

	r.GET("/punishment/player/:xuid", getPunishmentsFor)

	r.POST("/punishment/search", searchForPunishment)

	// Reports

	r.PUT("/report", reportPlayer)
	r.POST("/report", getReportsBulk)
	r.GET("/report/all-time", getBestReports)

	r.GET("/report/:xuid", getPlayerReport)
	r.DELETE("/report/:xuid", deleteReports)
	r.PATCH("/report/:xuid/markTraineeClaimed", markTraineeClaimed)
}
