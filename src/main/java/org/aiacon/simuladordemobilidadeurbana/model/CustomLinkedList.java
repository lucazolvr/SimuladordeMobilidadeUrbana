package org.aiacon.simuladordemobilidadeurbana.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

// Lista encadeada genérica
public class CustomLinkedList<T> implements Iterable<T> {

    // Classe interna para representar um nó da lista
    private class Node {
        T data;
        Node next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node first;
    private int size;

    public CustomLinkedList() {
        this.first = null;
        this.size = 0;
    }

    // Adicionar um item no final da lista
    public void add(T item) {
        Node newNode = new Node(item);
        if (first == null) {
            first = newNode;
        } else {
            Node current = first;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        size++;
    }

    // Obter o primeiro item da lista
    public T getFirst() {
        return first != null ? first.data : null;
    }

    // Retornar o tamanho da lista
    public int size() {
        return size;
    }

    // Verificar se a lista está vazia
    public boolean isEmpty() {
        return size == 0;
    }

    // Obter um item pelo índice (novo método)
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice inválido: " + index);
        }
        Node current = first;
        int count = 0;
        while (current != null) {
            if (count == index) {
                return current.data;
            }
            current = current.next;
            count++;
        }
        return null; // Não alcançado devido à validação do índice
    }

    // Retornar o índice do item (novo método)
    public int indexOf(T item) {
        Node current = first;
        int index = 0;

        while (current != null) {
            if (current.data.equals(item)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1; // Retorna -1 se o item não for encontrado
    }

    // Verificar se a lista contém um item
    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    // Remover o primeiro item da lista (novo método)
    public T removeFirst() {
        if (first == null) {
            return null;
        }
        T data = first.data;
        first = first.next;
        size--;
        return data;
    }

    // Remover um item específico da lista (novo método)
    public boolean remove(T item) {
        if (first == null) {
            return false;
        }

        if (first.data.equals(item)) {
            first = first.next;
            size--;
            return true;
        }

        Node current = first;
        while (current.next != null) {
            if (current.next.data.equals(item)) {
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }

        return false; // Item não encontrado
    }

    // Método para adicionar itens no início da lista
    public void addFirst(T item) {
        Node newNode = new Node(item);
        newNode.next = first;
        first = newNode;
        size++;
    }

    // Implementação do método iterator() para navegação
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node current = first;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("Não há mais elementos na lista.");
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}