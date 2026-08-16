package mongo

import (
	"Observer/model"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func (prov *MongoDataProvider) UpsertReport(report *model.PlayerReport) error {
	opts := options.Replace().SetUpsert(true)
	_, err := prov.Client.Database(databaseName).Collection("reports").ReplaceOne(context.TODO(), bson.M{"player": report.Player}, report, opts)

	return err
}

func (prov *MongoDataProvider) GetReports(players []string) (*[]model.PlayerReport, error) {
	result, err := prov.Client.Database(databaseName).Collection("reports").Find(context.TODO(), bson.M{"player": bson.M{"$in": players}})

	if err != nil {
		return nil, err
	}

	reports := []model.PlayerReport{}
	err = result.All(context.TODO(), &reports)

	if err != nil {
		return nil, err
	}

	return &reports, nil
}

func (prov *MongoDataProvider) GetReport(player string) (*model.PlayerReport, error) {
	result, err := prov.Client.Database(databaseName).Collection("reports").Find(context.TODO(), bson.M{"player": player})

	if err != nil {
		return nil, err
	}

	if result.Next(context.TODO()) {
		report := &model.PlayerReport{}
		err := result.Decode(report)
		if err != nil {
			return nil, err
		}

		return report, nil
	} else {
		return nil, nil
	}
}
