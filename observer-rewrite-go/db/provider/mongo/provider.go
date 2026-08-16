package mongo

import (
	"context"
	"errors"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"os"
)

const databaseName = "Observer"

type MongoDataProvider struct {
	Client           *mongo.Client
	Tracing          *mongo.Collection
	TracingWhitelist *mongo.Collection

	Reports        *mongo.Collection
	PlayerComments *mongo.Collection

	Reasons     *mongo.Collection
	Punishments *mongo.Collection
	Evidence    *mongo.Collection
	Usernames   *mongo.Collection
}

func (prov *MongoDataProvider) Start() error {
	uri, ok := os.LookupEnv("MONGO_URI")

	if !ok {
		return errors.New("MONGO_URI env is not set, cannot start MongoDB connection")
	}

	bsonOpts := &options.BSONOptions{
		UseJSONStructTags: true,
		NilSliceAsEmpty:   true,
	}

	client, err := mongo.Connect(context.TODO(), options.Client().ApplyURI(uri).SetBSONOptions(bsonOpts))

	if err != nil {
		return err
	}

	prov.Client = client

	db := client.Database(databaseName)

	prov.Tracing = db.Collection("alt_accounts")
	prov.TracingWhitelist = db.Collection("tracing_whitelists")

	prov.Reports = db.Collection("reports")
	prov.PlayerComments = db.Collection("player_comments")

	prov.Reasons = db.Collection("punishment_reasons")
	prov.Punishments = db.Collection("punishments")
	prov.Evidence = db.Collection("evidences")
	prov.Usernames = db.Collection("usernames")

	return nil
}

// ifElem is a very cursed shortcut for the fact that golang does not have ternary chaining operators (sadly)
// it returns elem if a full element was found, and otherwise it just returns an empty bson array
func ifElem(hasVal bool, elem bson.A) bson.A {
	if hasVal {

		return elem
	}

	return bson.A{}
}

func and(a bson.M, b bson.M) bson.M {
	return bson.M{
		"$and": []bson.M{
			a, b,
		},
	}
}
