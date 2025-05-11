package org.aiacon.simuladordemobilidadeurbana.model;

// Fila para veículos em semáforos
public class Queue {
    Vehicle front;
    Vehicle rear;

    public Queue() {
        front = null;
        rear = null;
    }

    public void enqueue(Vehicle vehicle) {
        if (rear == null) {
            front = vehicle;
            rear = vehicle;
        } else {
            rear.next = vehicle;
            rear = vehicle;
        }
    }

    public Vehicle dequeue() {
        if (front == null) return null;
        Vehicle vehicle = front;
        front = front.next;
        if (front == null) rear = null;
        vehicle.next = null;
        return vehicle;
    }

    public int size() {
        int count = 0;
        for (Vehicle v = front; v != null; v = v.next) {
            count++;
        }
        return count;
    }
}