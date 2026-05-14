export interface OrderItem {
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
}

export interface Order {
    orderId: number;
    status: string;
    paymentMethod: string;
    subtotal: number;
    discountTotal: number;
    totalAmount: number;
    items: OrderItem[],
    createdAt: string;
    paidAt: string;
    shippedAt: string;
    deliveredAt: string;
    cancelledAt: string;
}