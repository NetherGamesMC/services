package kafka

import "Observer/model"

func (kbp *KafkaBroadcastProvider) BroadcastReportUpserted(report *model.PlayerReport) error {
	return nil
}

func (kbp *KafkaBroadcastProvider) BroadcastReportProcessed(report *model.PlayerReport, resolution model.ReportResolution) error {
	return nil
}
