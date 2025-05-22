package org.aiacon.simuladordemobilidadeurbana.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

// Lista encadeada genérica
public class CustomLinkedList<T> implements Iterable<T> {

    // Classe interna para representar um nó da lista
    private static class Node<T> { // Tornando Node genérico também, boa prática
        T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node<T> first; // Ponteiro para o primeiro nó
    private Node<T> last;  // Ponteiro para o último nó (para add O(1))
    private int size;      // Mantém o tamanho da lista

    public CustomLinkedList() {
        this.first = null;
        this.last = null; // Inicializa last como null
        this.size = 0;
    }

    // Adicionar um item no final da lista - Agora O(1)
    public void add(T item) {
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) { // Se a lista está vazia, first e last apontam para o novo nó
            first = newNode;
            last = newNode;
        } else { // Adiciona após o último nó atual e atualiza last
            last.next = newNode;
            last = newNode;
        }
        size++;
    }

    // Método para adicionar itens no início da lista - O(1)
    public void addFirst(T item) {
        Node<T> newNode = new Node<>(item);
        newNode.next = first;
        first = newNode;
        if (last == null) { // Se a lista estava vazia, last também é o novo nó
            last = newNode;
        }
        size++;
    }

    // Obter o primeiro item da lista - O(1)
    public T getFirst() {
        if (isEmpty()) {
            // Lançar exceção é mais idiomático do que retornar null para "get" em lista vazia
            throw new NoSuchElementException("A lista está vazia.");
        }
        return first.data;
    }

    // Obter o último item da lista - O(1)
    public T getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException("A lista está vazia.");
        }
        return last.data;
    }

    // Retornar o tamanho da lista - O(1)
    public int size() {
        return size;
    }

    // Verificar se a lista está vazia - O(1)
    public boolean isEmpty() {
        return size == 0; // Ou first == null
    }

    // Obter um item pelo índice - O(N)
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice inválido: " + index + " para tamanho " + size);
        }
        Node<T> current = first;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    // Retornar o índice do item - O(N)
    public int indexOf(T item) {
        Node<T> current = first;
        int index = 0;
        while (current != null) {
            if (item == null) { // Lida com a busca por um item nulo
                if (current.data == null) {
                    return index;
                }
            } else {
                if (item.equals(current.data)) {
                    return index;
                }
            }
            current = current.next;
            index++;
        }
        return -1; // Item não encontrado
    }

    // Verificar se a lista contém um item - O(N)
    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    // Remover o primeiro item da lista - O(1)
    public T removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("Não é possível remover de uma lista vazia.");
        }
        T data = first.data;
        first = first.next;
        size--;
        if (isEmpty()) { // Se a lista ficou vazia, last também deve ser null
            last = null;
        }
        return data;
    }

    // Remover um item específico da lista - O(N)
    public boolean remove(T item) {
        if (isEmpty()) {
            return false;
        }

        // Caso o item a ser removido seja o primeiro
        if ((item == null && first.data == null) || (item != null && item.equals(first.data))) {
            removeFirst(); // Usa o método já existente que atualiza 'last' se necessário
            return true;
        }

        Node<T> current = first;
        while (current.next != null) {
            if ((item == null && current.next.data == null) || (item != null && item.equals(current.next.data))) {
                if (current.next == last) { // Se o nó a ser removido é o último
                    last = current; // O nó atual se torna o último
                }
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false; // Item não encontrado
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = first;

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

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Node<T> current = first;
        while (current != null) {
            sb.append(current.data == null ? "null" : current.data.toString());
            if (current.next != null) {
                sb.append(" -> ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}