export interface Product {
    id: number;
    name: string;
    description: string;
    price: number;
    totalStock?: number;
    reservedStock?: number;
    availableStock?: number;
    avaliableStock?: number;
    active?: boolean;
    createdAt?: string;
    categoryId: number;
    categoryName: string;
}
