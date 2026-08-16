package kafka

import (
	"github.com/IBM/sarama"
	"os"
	"time"
)

type KafkaBroadcastProvider struct {
	producer sarama.SyncProducer
}

func (kbp *KafkaBroadcastProvider) Init() error {
	config := sarama.NewConfig()
	config.Producer.Return.Successes = true
	config.Producer.Return.Errors = true
	producer, err := sarama.NewSyncProducer([]string{os.Getenv("KAFKA_ADDRESS")}, config)
	if err != nil {
		return err
	}

	kbp.producer = producer

	return nil
}

func (kbp *KafkaBroadcastProvider) broadcastMessage(topic string, key string, message []byte) error {
	_, _, err := kbp.producer.SendMessage(&sarama.ProducerMessage{
		Topic:     topic,
		Key:       sarama.StringEncoder(key),
		Value:     sarama.ByteEncoder(message),
		Timestamp: time.Now(),
	})

	return err
}
