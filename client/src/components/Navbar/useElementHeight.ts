import { useState, useRef, useEffect } from 'react';

const useElementHeight = () => {
    const elementRef = useRef(null);
    const [height, setHeight] = useState(0);

    useEffect(() => {
        if (elementRef.current) {
            const updateHeight = () => {
                setHeight(elementRef.current.getBoundingClientRect().height);
            };

            updateHeight();
            window.addEventListener('resize', updateHeight);

            return () => {
                window.removeEventListener('resize', updateHeight);
            };
        }
    }, [elementRef]);

    return [elementRef, height];
};

export default useElementHeight;
