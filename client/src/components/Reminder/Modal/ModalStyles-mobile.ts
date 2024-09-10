const ModalStylesMobile = (width) => {
    let modalHeight;
    let modalWidth;
    let modalGap;

    if (width > 730) {
        modalWidth = '600px';
        modalHeight = '550px';
        modalGap = '1.6rem';
    }
    else if (width > 500) {
        modalHeight = '500px';
        modalWidth = `${(width - 140).toString()}px`;
        modalGap = '2.0rem';
    }
    else if (width > 300) {
        modalHeight = '400px';
        modalWidth = `${(width - 140).toString()}px`;
        modalGap = '2.2rem';
    }
    else {
        modalHeight = '400px';
        modalWidth = '160px';
        modalGap = '2.8rem';
    }

    return {
        content: {
            width: modalWidth,
            height: modalHeight,
            margin: 'auto',
            borderRadius: '10px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: modalGap,
            backgroundColor: "#fbf9ef"
        },
        overlay: {
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
        }
    }
};

export default ModalStylesMobile;