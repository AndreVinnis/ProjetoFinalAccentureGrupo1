export interface CartItem {
    productId: number;
    productName: string;
    categoryName?: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
}

export interface Cart {
    cartId: number;
    subtotal: number;
    status?: string;
    items: CartItem[];
}
